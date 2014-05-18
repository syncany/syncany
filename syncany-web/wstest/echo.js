var secureCb;
var secureCbLabel;
var wsUri;
var consoleLog;
var connectBut;
var disconnectBut;
var sendMessage;
var sendBut;
var clearLogBut;

function echoHandlePageLoad() {
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
		sendMessage.value = "<getFileTreeRequest>\n  <id>123</id>\n  <root>/home/pheckel/Syncany</root>\n  <prefix></prefix>\n</getFileTreeRequest>";
	};

	document.getElementById("req2").onclick = function () {
		sendMessage.value = "<getFileRequest>\n  <id>123</id>\n  <root>/home/pheckel/Syncany</root>\n  <file>file1.txt</file>\n</getFileRequest>";
	};

	consoleLog = document.getElementById("consoleLog");

	clearLogBut = document.getElementById("clearLogBut");
	clearLogBut.onclick = clearLog;

	setGuiConnected(false);

	document.getElementById("disconnect").onclick = doDisconnect;
	document.getElementById("send").onclick = doSend;
}

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
}

function onClose(evt) {
	logToConsole("DISCONNECTED", "black");
	setGuiConnected(false);
}

var frames = [];
var framesExpected = 0;

function onMessage(evt) {
	if (evt.data instanceof Blob) {
		logToConsole('RESPONSE: Binary data received: ' + evt.data, 'blue');

		frames[frames.length] = evt.data;
		console.log(frames);

		if (frames.length == framesExpected) {
			var blob = new Blob(frames, { type: "application/octet-stream" });
			saveAs(blob, "somefile.png");

			frames = [];
		}
	} 
	else {
		logToConsole("RESPONSE:\n" + evt.data + "\n", "blue");
		console.log(evt);
		
		var xml = $(evt.data.toString());
		
		var framesXml = xml.find('frames');
		
		if (framesXml) {
			console.log(framesXml);
			framesExpected = xml.find('frames')[0].textContent;
		}
	}
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

window.addEventListener("load", echoHandlePageLoad, false);
