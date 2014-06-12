function Status(statusElements) {
	this.statusElements = statusElements;
	
	this.loading = function(text) {
		this.statusElements.html(
			"<span class='icon icon-loading'></span>" +
			"<span class='text text-loading'>"+text+"</span>"
		);
	};
	
	this.okay = function(text) {
		this.statusElements.html(
			"<span class='icon icon-okay'></span>" +
			"<span class='text text-okay'>"+text+"</span>"
		);
	};
	
	this.error = function(text) {
		this.statusElements.html(
			"<span class='icon icon-error'></span>" +
			"<span class='text text-error'>"+text+"</span>"
		);
	};
}
