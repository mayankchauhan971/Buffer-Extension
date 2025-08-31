function getTitle() {
    var title = document.title;
    var ogTitle = document.head && document.head.querySelector('meta[property="og:title"]');
    if (ogTitle && ogTitle.content && ogTitle.content.length) {
      title = ogTitle.content;
    }
    
    return title;
}

getTitle();
