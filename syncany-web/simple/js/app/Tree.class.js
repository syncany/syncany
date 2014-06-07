function Tree(tree) {
	this._init = function(tree) {
		this.tree = tree;
	}
	
	this._init_ui = function() {
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
			this._onFileClick(data);
	 	});
	 	
	 	this.tree = $.jstree.reference('#tree');
	}
	
	this.processFileTreeResponse = function(xml) {
		this.clearTree();
	
		if (prefix != "") {
			this.tree.create_node(null, {
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
			this.tree.create_node(null, {
				id: prefix + path,
				text: path,
				type: type,
				file: fileXml
			});
		});
	
		this.tree.scrollTop = 0;
	}
	
	this.clearTree = function() {
		var i=0;
		while (i++<1000) {
			var node = $("#tree").find('li');
	
			if (!node) {
				break;
			}
			else {
				this.tree.delete_node(node);
			}
		}
	}
	
	this._init(tree);
}


var prefix = "";
var prefixFile = null;

var root;
var rootSelect;

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

