var secureCb;
var secureCbLabel;
var wsUri;
var consoleLog;
var connectBut;
var disconnectBut;
var sendMessage;
var sendBut;
var clearLogBut;

var tree;

var prefix = "";
var prefixFile = null;

var root = "/home/pheckel/Syncany/Dropbox";
var rootSelect;

var frames = [];
var framesExpected = 0;
var mimeType = "";
var filename = "";

$(document).ready(function() {
	secureCb = document.getElementById("secureCb");
	secureCb.checked = false;
	secureCb.onclick = toggleTls;

	secureCbLabel = document.getElementById("secureCbLabel")

	wsUri = document.getElementById("wsUri");
	toggleTls();

	connectBut = document.getElementById("connect");
	connectBut.onclick = doConnect;

	disconnectBut = document.getElementById("disconnect");
	disconnectBut.onclick = doDisconnect;

	sendMessage = document.getElementById("sendMessage");

	sendBut = document.getElementById("send");
	sendBut.onclick = doSend;

	document.getElementById("req1").onclick = function () {
		sendMessage.value = "<getFileTreeRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <prefix>" + prefix + "</prefix>\n</getFileTreeRequest>";
	};

	document.getElementById("req2").onclick = function () {
		sendMessage.value = "<getFileRequest>\n  <id>123</id>\n  <root>" + root + "</root>\n  <file>file1.txt</file>\n</getFileRequest>";
	};
	
	document.getElementById("req3").onclick = function () {
		sendMessage.value = "<listWatchesRequest>\n  <id>123</id>\n</listWatchesRequest>";
	};

	consoleLog = document.getElementById("consoleLog");

	clearLogBut = document.getElementById("clearLogBut");
	clearLogBut.onclick = clearLog;

	setGuiConnected(false);

	document.getElementById("disconnect").onclick = doDisconnect;
	$("#send").click(doSend);
	
	rootSelect = $("#root");
	rootSelect.change(onRootSelect);
	
	$(function () {
		$(function () { 
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
		});
	});
	
	doConnect();
});

function toggleTls() {
	var wsPort = (window.location.port.toString() === "" ? "" : ":" + window.location.port)
	
	if (secureCb.checked) {
		wsUri.value = wsUri.value.replace("ws:", "wss:");
	} else {
		wsUri.value = wsUri.value.replace("wss:", "ws:");
	}
}

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
	logToConsole("SENT:\n" + sendMessage.value + "\n", "green");
	websocket.send(sendMessage.value);
}

function logToConsole(message, color) {
	var pre = document.createElement("p");
	pre.style.wordWrap = "break-word";
	pre.innerHTML = '<pre style="color: ' + color + ';">' + htmlEncode(message) + '</pre>';
	consoleLog.appendChild(pre);

	while (consoleLog.childNodes.length > 50) {
		consoleLog.removeChild(consoleLog.firstChild);
	}

	consoleLog.scrollTop = consoleLog.scrollHeight;
}

function onOpen(evt) {
	logToConsole("CONNECTED", "black");
	setGuiConnected(true);
	
	sendListWatchesRequest();
}

function onClose(evt) {
	logToConsole("DISCONNECTED", "black");
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
	logToConsole('RESPONSE: Binary data received: ' + evt.data, 'blue');

	frames[frames.length] = evt.data;
//	console.log(frames);

	if (frames.length == framesExpected) {
		logToConsole('RESPONSE: All blobs received, handing to browser.', 'blue');

		var blob = new Blob(frames, { type: mimeType });
		saveAs(blob, filename);

		frames = [];
	}
}

function processXmlMessage(evt) {
	logToConsole("RESPONSE:\n" + evt.data + "\n", "blue");
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
			else {
				logToConsole('WARNING: Unknown response: ' + evt.data.toString(), "blue");
			}
		}
		else {
			console.log(xml);
			console.log(codeXml);
			logToConsole('ERROR: Illegal response code: ' + codeXml, "red");
		}
	}
	else {
		logToConsole('ERROR: Illegal response: ' + evt.data.toString(), "red");
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
	frames = [];
	framesExpected = xml.find('frames').text();
	mimeType = xml.find('mimeType').text();
	filename = xml.find('name').text();
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


function onError(evt) {
	logToConsole('ERROR: ' + evt.data, "red");
}

function setGuiConnected(isConnected) {
	wsUri.disabled = isConnected;
	connectBut.disabled = isConnected;
	disconnectBut.disabled = !isConnected;
	sendMessage.disabled = !isConnected;
	sendBut.disabled = !isConnected;
	secureCb.disabled = isConnected;
	var labelColor = "black";
	if (isConnected) {
		labelColor = "#999999";
	}
	secureCbLabel.style.color = labelColor;

}

function clearLog() {
	while (consoleLog.childNodes.length > 0) {
		consoleLog.removeChild(consoleLog.lastChild);
	}
}

function htmlEncode( html ) {
    return document.createElement( 'a' ).appendChild( 
        document.createTextNode( html ) ).parentNode.innerHTML;
}
