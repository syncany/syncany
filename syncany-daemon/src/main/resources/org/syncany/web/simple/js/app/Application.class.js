function Application() {
	// ...
}

var wsUri;
var connectBut;
var disconnectBut;

var prefix = "";

var root;
var rootSelect;

var dateSelect;
var dateSelected;

var downloader;
var tree;
var status;
var table;

$(document).ready(function() {
	wsUri = document.getElementById("wsUri");

	connectBut = document.getElementById("connect");
	connectBut.onclick = doConnect;

	disconnectBut = document.getElementById("disconnect");
	disconnectBut.onclick = doDisconnect;

	document.getElementById("disconnect").onclick = doDisconnect;
	$("#send").click(sendMessage);
	
	$('#root').selectric();
	$('#menu').selectric();
	
	
		
	rootSelect = $("#root");
	rootSelect.change(onRootSelect);

	status = new Status($('#status'));
	tree = new Tree($('#tree'), onFileClick);
	table = new Table($('#table'), onContextFileInfoClick, onContextPreviousVersionsClick);
	downloader = new Downloader(status);
	dateSelect = new DateSelect($('#dateslider'), $('#datetext'), onDatabaseVersionHeaderChange);

	setGuiConnected(false);
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

function sendMessage(msg) {
	websocket.send(msg);
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
			else if (responseType == "getdatabaseversionheadersresponse") {
				processGetDatabaseVersionHeadersResponse(xml);
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
	status.loading('Updating tables');

	prefix = xml.find('prefix').text(); // new prefix!

	var fileVersions = toFileVersions(xml);
	
	table.populateTable(prefix, fileVersions);
	tree.populateTree(prefix, fileVersions);

	status.okay('All files in sync');
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

function processGetDatabaseVersionHeadersResponse(xml) {
	var datesXml = xml.find('date');
	var dates = $.map(datesXml, function(d) { return $(d).text(); });
	
	console.log(dates);
	dateSelect.updateSlider(dates);
}

function onRootSelect() {
	root = rootSelect.find("option:selected").first().val();
	console.log("new root: "+root);
	
	tree.clear(root);
	
	sendFileTreeRequest("");
	sendGetDatabaseVersionHeaders();
}

function onFileClick(file) {
	status.loading('Retrieving file list');
	
	if (file) {
		sendFileTreeRequest(file.path+"/");
	}
	else {
		sendFileTreeRequest("");
	}
}

function onContextFileInfoClick(file) {
	$("#dialog-fileinfo .text").html(file.path);
	
	$("#dialog-fileinfo").dialog({
		height: 140,
		modal: true
	});
}

function onContextPreviousVersionsClick(file) {
	$("#dialog-previous-versions").html("Retrieving file history for " + file.path + " ...");
	
	$("#dialog-previous-versions").dialog({
		height: 440,
		modal: true
	});
}

function onDatabaseVersionHeaderChange(databaseVersionHeader) {
	status.loading("Updating file tree");
	dateSelected = databaseVersionHeader;
	sendFileTreeRequest(prefix);
}

function onError(evt) {
	console.log('ERROR: ' + evt.data);
	status.error("Not connected");
}

function sendListWatchesRequest() {
	sendMessage("<listWatchesRequest>\n  <id>123</id>\n</listWatchesRequest>");
}

function sendFileTreeRequest(path) {
	sendMessage("<getFileTreeRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <prefix>" + path + "</prefix>\n  <date>" + dateSelected + "</date>\n</getFileTreeRequest>");
}

function sendGetDatabaseVersionHeaders() {
	sendMessage("<getDatabaseVersionHeadersRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n</getDatabaseVersionHeadersRequest>");
}

function retrieveFile(data) {
	window.location.assign("http://localhost:8080/api/rest/getFileRequest?id=123&root="+root+"&fileHistoryId="+data.fileHistoryId+"&version="+data.version);

	//sendMessage.value = "<getFileRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <file>" + fullpath + "</file>\n</getFileRequest>";
	//sendMessage();
}

function setGuiConnected(isConnected) {
	if (isConnected) {
		status.okay('Connected');
	}
	else {
		status.error('Not connected');
	}
}
