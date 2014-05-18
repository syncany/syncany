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
		sendMessage.value = '<getFileTreeRequest><id>123</id><root>/home/pheckel/Syncany</root><prefix></prefix></getFileTreeRequest>';
	};

	document.getElementById("req2").onclick = function () {
		sendMessage.value = '<getFileRequest><id>123</id><root>/home/pheckel/Syncany</root><file>file1.txt</file></getFileRequest>';
	};

	document.getElementById("req3").onclick = function () {
		sendMessage.value = '<getFileRequest><id>123</id><root>/home/pheckel/Syncany</root><file>file1.txt</file></getFileRequest>';
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
	logToConsole("SENT: " + sendMessage.value);
	websocket.send(sendMessage.value);
}

function logToConsole(message) {
	var pre = document.createElement("p");
	pre.style.wordWrap = "break-word";
	pre.innerHTML = message;
	consoleLog.appendChild(pre);

	while (consoleLog.childNodes.length > 50) {
		consoleLog.removeChild(consoleLog.firstChild);
	}

	consoleLog.scrollTop = consoleLog.scrollHeight;
}

function onOpen(evt) {
	logToConsole("CONNECTED");
	setGuiConnected(true);
}

function onClose(evt) {
	logToConsole("DISCONNECTED");
	setGuiConnected(false);
}

var blobs = [];

function onMessage(evt) {
	if (evt.data instanceof Blob) {
		logToConsole('<span style="color: blue;">RESPONSE: Binary data received: ' + evt.data + '</span>');

		blobs[blobs.length] = evt.data;
		console.log(blobs);

		if (blobs.length == 3) {
			var blob = new Blob(blobs, { type: "image/png" });
			saveAs(blob, "somefile.png");
		}
	} else {
		logToConsole('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
		console.log(evt);
	}
}


/*
WORKS FOR SMALL FILES:

function onMessage(evt) {
	logToConsole('<span style="color: blue;">RESPONSE: ' + evt.data+'</span>');
	console.log(evt);

	if (evt.data instanceof Blob) {
		console.log("isblob");
		reader.readAsDataURL(evt.data);
	}
}

function onFileReaderDataFinished(evt) {
	console.log("file reader data finished ");
	console.log(evt);

	file_contents = evt.target.result;
	document.getElementById("filecontents").value = document.getElementById("filecontents").value + evt.target.result;

	document.getElementById("filecontentsiframe").src = file_contents;
}*/

function onError(evt) {
	logToConsole('<span style="color: red;">ERROR:</span> ' + evt.data);
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

window.addEventListener("load", echoHandlePageLoad, false);
