console.log('Tree still depends on global variables prefix and prefixFile!!');

function Tree(treeElements, onFileClickCallback, onFileTreeNodeOpenCallback) {
	this.treeElements = treeElements;
	this.onFileClickCallback = onFileClickCallback;
	this.onFileTreeNodeOpenCallback = onFileTreeNodeOpenCallback;		
			
	this._init = function() {
		var onFileClickCallback = this.onFileClickCallback;
		var onFileTreeNodeOpenCallback = this.onFileTreeNodeOpenCallback;
		
		this.treeElements.jstree({
			'core' : {
				'data' : function (obj, cb) {
				    cb.call(this, []);
				},
				'check_callback' : true,
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
				'folder' : { 'valid_children': ['folder'], 'icon' : 'jstree-folder' },
				'file' : { 'valid_children' : [], 'icon' : 'jstree-file' }
			},
			'plugins' : ['sort', 'types'/*, 'wholerow'*/] 

		})
		.on("select_node.jstree", function (e, data) {
			onFileClickCallback(data);
	 	})
	 	.on("load_node.jstree", function (e, data) {
	 		onFileTreeNodeOpenCallback(data);
	 	});
	 	
	 	this.tree = $.jstree.reference('#'+this.treeElements[0].id);
	}
	
	this.processFileTreeResponse = function(xml) {
		var files = xml.find('files > file');
		var prefix = xml.find('prefix').text();
		
		var tree = this.tree;
		var parentNode = (prefix != "") ? tree.get_node(prefix.substr(0, prefix.length-1)) : null;

		$(files).each(function (i, file) {
			var fileXml = $(file);
			var path = fileXml.find('path').text();
			var type = fileXml.find('type').text().toLowerCase();
		
			var newNodeId = prefix + path;
			var newNode = tree.get_node(newNodeId);

			if (!newNode) {
				if (type == "folder") {
					tree.create_node(parentNode, {
						id: newNodeId,
						text: path,
						type: type,
						file: fileXml,
						children: true
					});
				}
			}
		});
		
		tree.open_node(parentNode);
		tree.scrollTop = 0;
	}
	
	this.clear = function() {
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


