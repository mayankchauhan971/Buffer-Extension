const match = matchMedia('(prefers-color-scheme: dark)');
chrome.runtime.sendMessage({ scheme: match.matches ? 'dark' : 'light' });
match.addListener(({ matches }) => {
  chrome.runtime.sendMessage({ scheme: matches ? 'dark' : 'light' });
});
