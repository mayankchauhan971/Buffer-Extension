$(document).ready(function() {
	$("#create-post").click(function() {
		chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
			var tab = tabs[0];
			openPostComposer(tab.id);
		});
	});

	$("#create-idea").click(function() {
		chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
			let tab = tabs[0];
			openIdeaComposer(tab.id);
		});
	});
	
	function openPostComposer(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "PUBLISHTAB"
		}
		
		chrome.runtime.sendMessage(message, function() {
			window.close();
		});
	}
	
	function openIdeaComposer(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "IDEASTAB"
		}
		
		chrome.runtime.sendMessage(message, function() {
			window.close();
		});
	}
});