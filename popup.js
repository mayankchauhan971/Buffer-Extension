$(document).ready(function() {
	let currentChatId = null;
	let currentUrl = null;

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

	$("#brainstorm-ai").click(function() {
		// Show chat interface
		$("#chat-container").show();
		$("#input-container").show();
		
		// Get current tab URL and selected text
		chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
			currentUrl = tabs[0].url;
			
			// Get selected text
			chrome.tabs.sendMessage(tabs[0].id, { action: "getSelection" }, function(response) {
				const selectedText = response ? response.text : '';
				
				// Make initial API call with URL and selected text
				makeApiCall({
					url: currentUrl,
					selectedText: selectedText,
					message: 'Initial request with URL and selected text'
				});
			});
		});
	});

	$("#send-button").click(function() {
		const message = $("#message-input").val().trim();
		if (message) {
			// Add user message to chat
			addMessageToChat(message, 'user');
			$("#message-input").val('');
			
			// Make API call with the message
			makeApiCall({
				chatId: currentChatId,
				message: message
			});
		}
	});

	function makeApiCall(data) {
		// For now, using the placeholder API
		fetch('https://jsonplaceholder.typicode.com/posts', {
			method: 'POST',
			body: JSON.stringify(data),
			headers: {
				'Content-type': 'application/json; charset=UTF-8',
			},
		})
		.then(response => response.json())
		.then(data => {
			// Simulate getting a chatId from the response
			// In real implementation, this would come from your API
			if (!currentChatId) {
				currentChatId = 'chat_' + Date.now();
			}
			
			// Add AI response to chat
			addMessageToChat(data.body, 'ai');
		})
		.catch(error => {
			addMessageToChat('Error: Failed to get AI response', 'ai');
			console.error('Error:', error);
		});
	}

	function addMessageToChat(message, sender) {
		const messageDiv = $('<div>')
			.addClass('chat-message')
			.addClass(sender === 'user' ? 'user-message' : 'ai-message')
			.text(message);
		
		$("#chat-container").append(messageDiv);
		$("#chat-container").scrollTop($("#chat-container")[0].scrollHeight);
	}
	
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