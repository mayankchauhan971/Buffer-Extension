$(document).ready(function() {
	var currentBrowser = (
		location.protocol === 'chrome-extension:' ? 'chrome' :
		location.protocol === 'moz-extension:' ? 'firefox' : ''
	);

	// Dynamically update page title and email subject
	var hardcodedBrowserName = 'Chrome';
	var formattedCurrentBrowser = currentBrowser.substr(0, 1).toUpperCase() + currentBrowser.substr(1);
	document.title = document.title.replace(hardcodedBrowserName, formattedCurrentBrowser);
	var emailAnchor = document.getElementById('email-anchor');
	emailAnchor.href = emailAnchor.href.replace(hardcodedBrowserName, formattedCurrentBrowser);


	// Perform migration to browser.storage.local. Previously the extension used localStorage which isn't 
	// available with Manifest V3

	var store = {};

	if (localStorage.getItem('buffer.op.hacker')) {
		store['buffer.op.hacker'] = localStorage.getItem('buffer.op.hacker');
	}

	if (localStorage.getItem('buffer.op.image-overlays')) {
		store['buffer.op.image-overlays'] = localStorage.getItem('buffer.op.image-overlays');
	}

	if (localStorage.getItem('buffer.op.key-combo')) {
		store['buffer.op.key-combo'] = localStorage.getItem('buffer.op.key-combo');
	}

	if (localStorage.getItem('buffer.op.key-enable')) {
		store['buffer.op.key-enable'] = localStorage.getItem('buffer.op.key-enable');
	}

	if (localStorage.getItem('buffer.op.pinterest')) {
		store['buffer.op.pinterest'] = localStorage.getItem('buffer.op.pinterest');
	}

	if (localStorage.getItem('buffer.op.pinterest')) {
		store['buffer.op.pinterest'] = localStorage.getItem('buffer.op.pinterest');
	}

	if (localStorage.getItem('buffer.op.reddit')) {
		store['buffer.op.reddit'] = localStorage.getItem('buffer.op.reddit');
	}

	if (localStorage.getItem('buffer.op.tweetdeck')) {
		store['buffer.op.tweetdeck'] = localStorage.getItem('buffer.op.tweetdeck');
	}

	if (localStorage.getItem('buffer.op.twitter')) {
		store['buffer.op.twitter'] = localStorage.getItem('buffer.op.twitter');
	}

	if(Object.keys(store).length != 0){
		store['migrated'] = true;
		
		chrome.storage.local.set(store, function() {
			// Indicate to the user that things went well
			$(this).text('Saved').addClass("saved");
	
			window.close();
		});
	} else {
		// Defaults
		var store = {};
		store['buffer.op.hacker'] = 'hacker';
		store['buffer.op.image-overlays'] = 'image-overlays';
		store['buffer.op.key-combo'] = 'alt+b';
		store['buffer.op.key-enable'] = 'key-enable';
		store['buffer.op.pinterest'] = 'pinterest';
		store['buffer.op.pinterest'] = 'pinterest';
		store['buffer.op.reddit'] = 'reddit';
		store['buffer.op.tweetdeck'] = 'tweetdeck';
		store['buffer.op.twitter'] = 'twitter';
		store['migrated'] = true;
		
		chrome.storage.local.set(store, function() {
			
		});
		
		window.close();
	}
});