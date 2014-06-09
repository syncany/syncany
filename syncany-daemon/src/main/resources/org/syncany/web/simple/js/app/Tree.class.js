console.log('Tree still depends on global variables prefix and prefixFile!!');

function Tree(treeElements, onFileClickCallback) {
	this.treeElements = treeElements;
	this.onFileClickCallback = onFileClickCallback;
			
	this._init = function() {
		var onFileClickCallback = this.onFileClickCallback;
		
		this.treeElements.jstree({
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
			onFileClickCallback(data);
	 	});
	 	
	 	this.tree = $.jstree.reference('#'+this.treeElements[0].id);
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
		var tree = this.tree;

		$(files).each(function (i, file) {
			var fileXml = $(file);
			var path = fileXml.find('path').text();
			var type = fileXml.find('type').text().toLowerCase();
		
			if (type == "folder") {
				tree.create_node(null, {
					id: prefix + path,
					text: path,
					type: type,
					file: fileXml
				});
			}
			else {
				/*if (type == "symlink") type = "file";
		
				console.log(file);
				tree.create_node(null, {
					id: prefix + path,
					text: path,
					type: type,
					file: fileXml
				});*/
			}
		});
	
		this.tree.scrollTop = 0;
	}
	
	this.clearTree = function() {
		var i=0;
		while (i++<1000) {
			var node = this.treeElements.find('li');
	
			if (!node) {
				break;
			}
			else {
				this.tree.delete_node(node);
			}
		}
	}
	
	this._init();
}


