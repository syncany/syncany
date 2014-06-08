function htmlEncode(html) {
	return document.createElement('a')
		.appendChild(document.createTextNode(html)).parentNode.innerHTML;
}

function basename(path) {
	return path.split(/[\\/]/).pop();
}
