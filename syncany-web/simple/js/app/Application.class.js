function Application() {
	// ...
}

var wsUri;
var connectBut;
var disconnectBut;
var sendMessage;
var sendBut;

var prefix = "";
var prefixFile = null;

var root;
var rootSelect;

var downloader;
var tree;
var status;

$(document).ready(function() {

	wsUri = document.getElementById("wsUri");

	connectBut = document.getElementById("connect");
	connectBut.onclick = doConnect;

	disconnectBut = document.getElementById("disconnect");
	disconnectBut.onclick = doDisconnect;

	sendMessage = document.getElementById("sendMessage");

	sendBut = document.getElementById("send");
	sendBut.onclick = doSend;

	setGuiConnected(false);

	document.getElementById("disconnect").onclick = doDisconnect;
	$("#send").click(doSend);
	
	$('#root').selectric();
	$('#menu').selectric();
	
	
	
	rootSelect = $("#root");
	rootSelect.change(onRootSelect);

	status = new Status($('#status'));
	tree = new Tree($('#tree'), onFileClick);
	downloader = new Downloader(status);
	
	doConnect();
});

function doConnect() {
	var uri = wsUri.value;

	websocket = new WebSocket(uri);
	websocket.onopen = function (evt) { onOpen(evt) };
	websocket.onclose = function (evt) { onClose(evt) };
	websocket.onmessage = function (evt) { onMessage(evt) };
	websocket.onerror = function (evt) { onError(evt) };
	
	status.loading("Connecting to Syncany ...");
}

function doDisconnect() {
	websocket.close()
	status.okay("Disconnected");
}

function doSend() {
	websocket.send(sendMessage.value);
}

function onOpen(evt) {
	setGuiConnected(true);
	status.okay("Connected");

	sendListWatchesRequest();
}

function onClose(evt) {
	setGuiConnected(false);
}

function onMessage(evt) {
	if (evt.data instanceof Blob) {
		processBlobMessage(evt);
	} 
	else {
		processXmlMessage(evt);
	}
}

function processBlobMessage(evt) {
	downloader.processBlobMessage(evt.data);
}

function processXmlMessage(evt) {
	console.log(evt);
	
	var xml = $(evt.data.toString());

	if (xml && xml[0]) {		
		var responseType = xml[0].nodeName.toString().toLowerCase();
		var codeXml = xml.find('code');

		if (codeXml && codeXml.text() == 200) {	
			if (responseType == "getfiletreeresponse") {
				processFileTreeResponse(xml);
			}
			else if (responseType == "getfileresponse") {
				processFileResponse(xml);
			}
			else if (responseType == "listwatchesresponse") {
				processListWatchesResponse(xml);
			}
			else if (responseType == "watcheventresponse") {
				processWatchEventResponse(xml);
			}
			else {
				console.log('WARNING: Unknown response: ' + evt.data.toString());
			}
		}
		else {
			console.log(xml);
			console.log(codeXml);
			console.log('ERROR: Illegal response code: ' + codeXml);
		}
	}
	else {
		console.log('ERROR: Illegal response: ' + evt.data.toString());
	}
}

function processFileTreeResponse(xml) {
	status.okay('All files in sync');

	populateDataTable(xml);
	tree.processFileTreeResponse(xml);
}

function populateDataTable(xml) {
	$('#table').dataTable().fnDestroy();
	var fileVersions = toFileVersions(xml);

	$('#table').dataTable({
		paging: false,
		searching: false,
		jQueryUI: false,
		info: false,
		ordering: true,
		data: fileVersions,
		columns: [
			{ data: 'path', orderData: [2, 0], target: 1 },
			{ data: 'version' },
			{ data: 'type', visible: false },
			{ data: 'status' },
			{ data: 'size' },
			{ data: 'lastModified' },
			{ data: 'posixPermissions' },
		],
		createdRow: function ( row, data, index ) {
			if (data.type == "FOLDER") {
				$('td', row).eq(0).prepend("<span class='folder'></span>");
			}
			else {
				$('td', row).eq(0).prepend("<span class='file'></span>");
			}
		}
	});
}

function toFileVersions(xml) {
	var fileElements = xml.find('files > file');
	var fileVersions = [];
		
	$(fileElements).each(function (i, file) {
		var fileXml = $(file);

		fileVersions.push(new FileVersion(
			fileXml.find('version').text(),
			fileXml.find('path').text(),
			fileXml.find('type').text(),
			fileXml.find('status').text(),
			fileXml.find('size').text(),
			fileXml.find('lastModified').text(),
			"", // TODO fix checksum formatting
			fileXml.find('updated').text(),
			fileXml.find('posixPermissions').text(),
			"" // TODO fix DOS attrs
		));
	});
	
	console.log(fileVersions);
	return fileVersions;
}

function processFileResponse(xml) {
	downloader.setMetaData(
		xml.find('name').text(), 
		xml.find('length').text(), 
		xml.find('mimeType').text(),
		xml.find('frames').text()
	);
}

function processWatchEventResponse(xml) {
	status.loading(xml.find('action'));
}

function processListWatchesResponse(xml) {
	rootSelect.find('option').remove();

	var watches = xml.find('watches > watch');
	
	$(watches).each(function (i, watch) {
		console.log($(watch).text());
		
		var rootPath = $(watch).text();
		rootSelect.append($("<option />").val(rootPath).text(basename(rootPath)));
	});
	
	$('#root').selectric('refresh');
	onRootSelect();
}

function onRootSelect() {
	root = rootSelect.find("option:selected").first().val();
	prefix = "";
	console.log("new root: "+root);
	
	sendFileTreeRequestGoUp();
}

function onFileClick(data) {
	status.loading('Retrieving file list ...');
	prefixFile = data.node.original.file;
	
	var fileXml = data.node.original.file;
	var path = fileXml.find('path').text();
	var type = fileXml.find('type').text().toLowerCase();

	if (data.node.type == "up") {
		sendFileTreeRequestGoUp();
	}
	else {
		if (type == "file" || type == "symlink") {
			retrieveFile(path);
		}
		else if (type == "folder") {
			sendFileTreeRequest(path);
		}
	}
}

function onError(evt) {
	console.log('ERROR: ' + evt.data);
	status.okay("Not connected");

}

function sendListWatchesRequest() {
	sendMessage.value = "<listWatchesRequest>\n  <id>123</id>\n</listWatchesRequest>";
	doSend();
}

function sendFileTreeRequest(path) {
	prefix = (prefix != "") ? prefix + path + "/" : path + "/";
	sendMessage.value = "<getFileTreeRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <prefix>" + prefix + "</prefix>\n</getFileTreeRequest>";
	doSend();
}

function sendFileTreeRequestGoUp() {
	prefix = (prefix != "") ? prefix.substr(0, prefix.substr(0, prefix.length-1).lastIndexOf('/')+1) : prefix;
	sendMessage.value = "<getFileTreeRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <prefix>" + prefix + "</prefix>\n</getFileTreeRequest>";
	doSend();
}

function retrieveFile(path) {
	fullpath = (prefix != "") ? prefix + path : path;
	sendMessage.value = "<getFileRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <file>" + fullpath + "</file>\n</getFileRequest>";
	doSend();
}

function setGuiConnected(isConnected) {
	wsUri.disabled = isConnected;
	connectBut.disabled = isConnected;
	disconnectBut.disabled = !isConnected;
	sendMessage.disabled = !isConnected;
	sendBut.disabled = !isConnected;
	var labelColor = "black";
	if (isConnected) {
		labelColor = "#999999";
	}

}
