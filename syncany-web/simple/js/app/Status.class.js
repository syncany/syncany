function Status(statusElements) {
	this.statusElements = statusElements;
	
	this.loading = function(text) {
		this.statusElements.html(
			"<span class='status-loading'></span>" +
			"<span class='status-text'>"+text+"</span>"
		);
	};
	
	this.okay = function(text) {
		this.statusElements.html(
			"<span class='status-okay'></span>" +
			"<span class='status-text'>"+text+"</span>"
		);
	};
}
