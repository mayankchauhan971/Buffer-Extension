/**
 * Extract comprehensive page content for AI processing
 * Executed as a content script to gather page data
 */
function getPageContentForAI() {
  try {
    // Get all visible text content
    const fullText = document.body.innerText || document.body.textContent || '';
    
    // Get page metadata
    const title = document.title || '';
    const url = window.location.href;
    
    // Get page description
    const descriptionMeta = document.querySelector('meta[name="description"]') || 
                           document.querySelector('meta[property="og:description"]');
    const description = descriptionMeta ? descriptionMeta.content : '';
    
    // Get headings for structure
    const headings = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6'))
      .map(h => h.textContent.trim())
      .filter(text => text.length > 0);
    
    // Calculate content stats
    const wordCount = fullText.split(/\s+/).filter(word => word.length > 0).length;
    
    return {
      fullText: fullText.trim(),
      title: title.trim(),
      url: url,
      description: description.trim(),
      headings: headings,
      wordCount: wordCount,
      extractedAt: new Date().toISOString()
    };
    
  } catch (error) {
    console.error('Content extraction error:', error);
    return {
      fullText: '',
      title: document.title || '',
      url: window.location.href,
      description: '',
      headings: [],
      wordCount: 0,
      error: error.message
    };
  }
}

getPageContentForAI(); 