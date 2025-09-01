

import React from 'react';
import { createRoot } from 'react-dom/client';
import SidepanelBrainstormFlow from './SidepanelBrainstormFlow';

window.initializeBrainstormFlow = function(pageContent = null, cachedApiResponse = null, forceRefresh = false) {
  const container = document.getElementById('brainstorm-root');
  
  if (container) {
    container.innerHTML = '';
    const root = createRoot(container);
    root.render(<SidepanelBrainstormFlow pageContent={pageContent} cachedApiResponse={cachedApiResponse} forceRefresh={forceRefresh} />);
  } else {
    console.error('brainstorm-root container not found in DOM');
  }
};
