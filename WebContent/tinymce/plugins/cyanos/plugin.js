tinymce.PluginManager.add('cyanos', function(editor, url) {
    // Add a button that opens a window
    editor.addButton('cyanos', {
        text: 'Link Objects',
        icon: false,
        onclick: function() {
            // Open window
            editor.windowManager.open({
                title: 'Cyanos objects',
                url: 'link.jsp'
            });
        }
    });

    editor.addButton('addfile', {
        text: 'Add File',
        icon: false,
        onclick: function() {
            // Open window
            editor.windowManager.open({
                title: 'Add File',
                url: 'link-file.jsp'
            });
        }
    });

    // Adds a menu item to the tools menu
    editor.addMenuItem('cyanos', {
        text: 'Cyanos',
        context: 'tools',
        onclick: function() {
            // Open window with a specific url
            editor.windowManager.open({
                title: 'TinyMCE site',
                url: 'http://www.tinymce.com',
                width: 800,
                height: 600,
                buttons: [{
                    text: 'Close',
                    onclick: 'close'
                }]
            });
        }
    });
});