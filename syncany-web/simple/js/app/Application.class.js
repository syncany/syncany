function Application() {
	// ...
}


var wsUri;
var connectBut;
var disconnectBut;
var sendMessage;
var sendBut;

var tree;

var prefix = "";
var prefixFile = null;

var root;
var rootSelect;

var downloader = new Downloader();
var treex;

$(document).ready(function() {
	//treex = new Tree($('#tree'));

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
	
	rootSelect = $("#root");
	rootSelect.change(onRootSelect);

	
	$('#tree').jstree({
		'core' : {
			'data' : function (obj, cb) {
			    cb.call(this, []);
			},
			'check_callback' : function(o, n, p, i, m) {
				return true;
			},
			'themes' : {
				'responsive' : false,
				'variant' : 'medium',
				'stripes' : true
			}
		},
		'sort' : function(a, b) {
			return this.get_type(a) === this.get_type(b) ? (this.get_text(a) > this.get_text(b) ? 1 : -1) : (this.get_type(a) >= this.get_type(b) ? -1 : 1);
		},
		'types' : {
			'up' : { 'icon' : 'jstree-folder' },
			'folder' : { 'icon' : 'jstree-folder' },
			'file' : { 'valid_children' : [], 'icon' : 'jstree-file' }
		},
		'plugins' : ['sort', 'types', 'wholerow'] 

	})
	.on("select_node.jstree", function (e, data) {
		onFileClick(data);
 	});
 	
 	tree = $.jstree.reference('#tree');
	
	doConnect();
});

function doConnect() {
	var uri = wsUri.value;

	websocket = new WebSocket(uri);
	websocket.onopen = function (evt) { onOpen(evt) };
	websocket.onclose = function (evt) { onClose(evt) };
	websocket.onmessage = function (evt) { onMessage(evt) };
	websocket.onerror = function (evt) { onError(evt) };
}

function doDisconnect() {
	websocket.close()
}

function doSend() {
	websocket.send(sendMessage.value);
}

function onOpen(evt) {
	setGuiConnected(true);
	
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
	clearTree();
	
	if (prefix != "") {
		tree.create_node(null, {
			id: "up",
			text: "..",
			type: "up",
			file: prefixFile
		});
	}
		
	var files = xml.find('files > file');

	$(files).each(function (i, file) {
		var fileXml = $(file);
		var path = fileXml.find('path').text();
		var type = fileXml.find('type').text().toLowerCase();
		
		if (type == "symlink") type = "file";
		
		console.log(file);
		tree.create_node(null, {
			id: prefix + path,
			text: path,
			type: type,
			file: fileXml
		});
	});
	
	tree.scrollTop = 0;
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
	$('#status').html(xml.find('action'));
}

function processListWatchesResponse(xml) {
	rootSelect.find('option').remove();

	var watches = xml.find('watches > watch');
	
	$(watches).each(function (i, watch) {
		console.log($(watch).text());
		
		var rootPath = $(watch).text();
		rootSelect.append($("<option />").val(rootPath).text(rootPath));
	});
	
	onRootSelect();
}

function onRootSelect() {
	root = rootSelect.find("option:selected").first().text();
	prefix = "";
	console.log("new root: "+root);
	
	sendFileTreeRequestGoUp();
}

function onFileClick(data) {
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

function clearTree() {
	var i=0;
	while (i++<1000) {
		var node = $("#tree").find('li');
	
		if (!node) {
			break;
		}
		else {
			tree.delete_node(node);
		}
	}
}

function onError(evt) {
	console.log('ERROR: ' + evt.data);
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
