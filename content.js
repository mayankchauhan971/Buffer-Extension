chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if (request.action === "getSelection") {
        const selectedText = window.getSelection().toString();
        sendResponse({ text: selectedText });
    }
    return true;
}); 