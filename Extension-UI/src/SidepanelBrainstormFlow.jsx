import React, { useState, useEffect, useRef } from 'react';
import MindMapFlow from './MindMapFlow';
import BouncingLoader from './BouncingLoader';

// Development mode toggle - set to true to use mock response instead of API
const DEVELOPMENT_MODE = false;

const API_CONFIG = {
  endpoint: 'http://localhost:8080/api/context', 
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000
};

/**
 * Load mock response for development mode
 */
const loadMockResponse = async () => {
  try {
    const response = await fetch(chrome.runtime.getURL('mock-response.json'));
    return await response.json();
  } catch (error) {
    console.error('Failed to load mock response:', error);
    throw error;
  }
};

/**
 * Main brainstorm flow component that handles API calls and displays results
 */
function SidepanelBrainstormFlow({ pageContent, cachedApiResponse = null, forceRefresh = false }) {
  const [apiStatus, setApiStatus] = useState(cachedApiResponse && !forceRefresh ? 'success' : 'idle'); 
  const [apiResponse, setApiResponse] = useState(forceRefresh ? null : cachedApiResponse);
  const [apiError, setApiError] = useState(null);
  
  // Track the current request's AbortController to allow cancellation
  const currentRequestRef = useRef(null);

  /**
   * Create payload for context API
   */
  const createContentPayload = (content) => {
    return {
      title: content.title || '',
      fullText: content.fullText || '',
      description: content.description || '',
      url: content.url || '',
      headings: content.headings || [],
      channels: ["Instagram", "X", "LinkedIn"]
    };
  };

  /**
   * Call the context API endpoint
   */
  const callContextEndpoint = async (content) => {
    try {
      // Cancel any existing request before starting a new one
      if (currentRequestRef.current) {
        console.log('Cancelling previous API request');
        currentRequestRef.current.abort();
        currentRequestRef.current = null;
      }

      setApiStatus('calling');
      setApiError(null);
      
      const payload = createContentPayload(content);
      let result;
      
      if (DEVELOPMENT_MODE) {
        // Use mock response in development mode with artificial delay
        const controller = new AbortController();
        currentRequestRef.current = controller; // Store reference for potential cancellation
        
        const delayPromise = new Promise((resolve, reject) => {
          const timeoutId = setTimeout(resolve, 5000); // 5 second delay
          
          // Handle cancellation
          controller.signal.addEventListener('abort', () => {
            clearTimeout(timeoutId);
            reject(new Error('AbortError'));
          });
        });
        
        const [mockResult] = await Promise.all([
          loadMockResponse(),
          delayPromise
        ]);
        result = mockResult;
      } else {
        // Make actual API call in production mode
        const controller = new AbortController();
        currentRequestRef.current = controller; // Store reference for potential cancellation
        
        const timeoutId = setTimeout(() => controller.abort(), API_CONFIG.timeout);
        
        const response = await fetch(API_CONFIG.endpoint, {
          method: API_CONFIG.method,
          headers: API_CONFIG.headers,
          body: JSON.stringify(payload),
          signal: controller.signal
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(`HTTP ${response.status}: ${response.statusText}${errorText ? ` - ${errorText}` : ''}`);
        }
        
        result = await response.json();
      }
     
      // Clear the request reference since it completed successfully
      currentRequestRef.current = null;
      
      setApiStatus('success');
      setApiResponse(result);
      
      // Cache the API response
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) {
          chrome.runtime.sendMessage({
            action: "CACHE_API_RESPONSE",
            data: {
              tabId: tabs[0].id,
              url: tabs[0].url,
              apiResponse: result
            }
          });
        }
      });
      
    } catch (error) {
      // Clear the request reference
      currentRequestRef.current = null;
      
      // Don't update state if the request was aborted (cancelled)
      if (error.name === 'AbortError') {
        console.log('API request was cancelled');
        return;
      }
      
      setApiStatus('error');
      setApiError(error.message);
    }
  };

  useEffect(() => {
    if (pageContent) {
      if (pageContent.error) {
        console.error('Extraction Error:', pageContent.error);
      }
      
      if (cachedApiResponse && !forceRefresh) {
        // Using cached response
      } else {
        callContextEndpoint(pageContent);
      }
    }
  }, [pageContent, cachedApiResponse, forceRefresh]);

  // Cleanup: Cancel any pending request when component unmounts
  useEffect(() => {
    return () => {
      if (currentRequestRef.current) {
        console.log('Cancelling API request due to component unmount');
        currentRequestRef.current.abort();
        currentRequestRef.current = null;
      }
    };
  }, []);

  /**
   * Render content based on current API status
   */
  const renderContent = () => {
    switch (apiStatus) {
      case 'calling':
        return (
          <BouncingLoader 
            message="Buffer-ing"
            subMessage="Analyzing content and generating ideas..."
            size="medium"
          />
        );
      
      case 'success':
        return apiResponse && apiResponse.channels ? (
          <MindMapFlow data={apiResponse} />
        ) : (
          <div style={{ padding: '20px', textAlign: 'center' }}>
            <div style={{ 
              fontSize: '24px', 
              marginBottom: '20px',
              color: '#00C851'
            }}>
              ✅
            </div>
            <div style={{ 
              fontSize: '18px', 
              fontWeight: '500', 
              color: '#00C851',
              marginBottom: '10px'
            }}>
              Analysis Complete!
            </div>
            <div style={{ 
              fontSize: '14px', 
              color: '#666',
              marginBottom: '10px'
            }}>
              {apiResponse?.summary || 'Context endpoint processed the page content'}
            </div>
            {apiResponse?.chatID && (
              <div style={{ 
                fontSize: '12px', 
                color: '#2c4bff',
                marginBottom: '20px',
                fontFamily: 'monospace'
              }}>
                💬 Chat ID: {apiResponse.chatID}
              </div>
            )}
          </div>
        );
      
      case 'error':
        return (
          <div style={{ padding: '20px', textAlign: 'center' }}>
            <div style={{ 
              fontSize: '24px', 
              marginBottom: '20px',
              color: '#ff4444'
            }}>
              ❌
            </div>
            <div style={{ 
              fontSize: '18px', 
              fontWeight: '500', 
              color: '#ff4444',
              marginBottom: '10px'
            }}>
              Something went wrong
            </div>
            <div style={{ 
              fontSize: '14px', 
              color: '#666',
              marginBottom: '20px'
            }}>
              {apiError || 'Unknown error occurred'}
            </div>
            <button 
              onClick={() => pageContent && callContextEndpoint(pageContent)}
              style={{
                background: '#2c4bff',
                color: 'white',
                border: 'none',
                padding: '10px 20px',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              Try Again
            </button>
          </div>
        );
      
      default:
        return (
          <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>
            <div style={{ fontSize: '18px', marginBottom: '10px' }}>
              📄 Ready to process
            </div>
            <div style={{ fontSize: '14px' }}>
              Waiting for content extraction...
            </div>
          </div>
        );
    }
  };

  return (
    <div style={{ 
      width: '100%', 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      fontFamily: "'Roboto', sans-serif"
    }}>
      <div style={{ 
        flex: 1,
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center'
      }}>
        {renderContent()}
      </div>
    </div>
  );
}

export default SidepanelBrainstormFlow; 