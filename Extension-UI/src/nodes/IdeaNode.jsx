import React from 'react';
import { Handle, Position, useReactFlow } from '@xyflow/react';

// Define consistent center position for all nodes
const DEFAULT_CENTER_POSITION = {
  x: -100,  // Horizontal center
  y: -150  // Slightly above screen center for better visual appearance
};

const IdeaNode = ({ data, id }) => {
  const { contentIdea, ideaIndex, socialMediaChannel, isFromTopChannel } = data;
  const { setCenter, getZoom, getNode } = useReactFlow();
  
  const handleNodeClick = (e) => {
    e.stopPropagation();
    // Return to the same view as when page first loads (fitView)
    const { fitView } = useReactFlow();
    
    fitView({ 
      padding: 0.3,
      includeHiddenNodes: false,
      duration: 600,
      minZoom: 0.5,
      maxZoom: 1.2
    });
  };

  const handleCreatePost = (e) => {
    e.stopPropagation(); // Prevent node click
    chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
      const tab = tabs[0];
      
      // Format the contentIdea content for posting
      const ideaContent = `${contentIdea.idea || 'No content available'}

Why this might work: ${contentIdea.rationale || 'No rationale provided'}

Pros: ${formatListItems(contentIdea.pros)}
Cons: ${formatListItems(contentIdea.cons)}`;

      const message = {
        data: {
          text: ideaContent,
          url: tab.url,
          placement: "idea_node"
        },
        action: "PUBLISH"
      };
      
      chrome.runtime.sendMessage(message, function() {
        // Side panel stays open
      });
    });
  };

  const handleSaveIdea = (e) => {
    e.stopPropagation(); // Prevent node click
    chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
      const tab = tabs[0];
      
      // Format the contentIdea content for saving
      const ideaContent = `${contentIdea.idea || 'No content available'}

Why this might work: ${contentIdea.rationale || 'No rationale provided'}

Pros: ${formatListItems(contentIdea.pros)}
Cons: ${formatListItems(contentIdea.cons)}

Source URL: ${tab.url}`;

      const message = {
        data: {
          text: ideaContent,
          placement: "idea_node"
        },
        action: "IDEA"
      };
      
      chrome.runtime.sendMessage(message, function() {
        // Side panel stays open
      });
    });
  };

  const formatListItems = (items) => {
    if (!items || items.length === 0) return 'None listed';
    return items.join(', ');
  };

  return (
    <div 
      onClick={handleNodeClick}
      style={{
        background: '#ffffff',
        border: '2px solid #e9ecef',
        borderRadius: '12px',
        padding: '16px',
        width: '280px',
        fontSize: '13px',
        boxShadow: '0 3px 10px rgba(0,0,0,0.1)',
        position: 'relative',
        cursor: 'pointer',
        transition: 'all 0.2s ease'
      }}
    >
      {/* Header */}
      <div style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        color: 'white',
        padding: '8px 12px',
        borderRadius: '8px',
        fontSize: '14px',
        fontWeight: 'bold',
        textAlign: 'center',
        marginBottom: '16px'
      }}>
        {socialMediaChannel.charAt(0).toUpperCase() + socialMediaChannel.slice(1)} Post Idea - {ideaIndex + 1}
      </div>

      {/* Content - Show all without scrolling */}
      <div style={{
        fontSize: '13px',
        lineHeight: '1.5',
        color: '#333'
      }}>
        {/* Content Paragraph */}
        <p style={{ marginBottom: '14px' }}>
          <p style={{ color: '#6c757d', fontSize: '14px' }}><strong>📝 What's the idea?</strong></p>
          <p>{contentIdea.idea || 'No content available'}</p>
        </p>

        {/* Rationale Paragraph */}
        <p style={{ marginBottom: '14px' }}>
          <p style={{ color: '#6c757d', fontSize: '14px' }}><strong>🎯 Why this might work?</strong></p>
          {contentIdea.rationale || 'No rationale provided'}
        </p>

        {/* Pros Paragraph */}
        <p style={{ marginBottom: '14px' }}>
          <strong style={{ color: '#28a745', fontSize: '14px' }}>✅ Pros:</strong> {formatListItems(contentIdea.pros)}
        </p>

        {/* Cons Paragraph */}
        <p style={{ marginBottom: '16px' }}>
          <strong style={{ color: '#dc3545', fontSize: '14px' }}>❌ Cons:</strong> {formatListItems(contentIdea.cons)}
        </p>
      </div>

      {/* Action Buttons */}
      <div style={{
        display: 'flex',
        gap: '8px',
        marginTop: '16px'
      }}>
        <button
          onClick={handleCreatePost}
          style={{
            flex: 1,
            height: '36px',
            borderRadius: '4px',
            background: '#2c4bff',
            color: '#fff',
            border: 'none',
            fontSize: '12px',
            fontWeight: '500',
            cursor: 'pointer',
            transition: 'background 0.2s ease'
          }}
          onMouseEnter={(e) => e.target.style.background = '#1e3bdd'}
          onMouseLeave={(e) => e.target.style.background = '#2c4bff'}
        >
          Create Post
        </button>
        <button
          onClick={handleSaveIdea}
          style={{
            flex: 1,
            height: '36px',
            borderRadius: '4px',
            background: '#fff',
            color: '#636363',
            border: '1px solid #b8b8b8',
            fontSize: '12px',
            fontWeight: '500',
            cursor: 'pointer',
            transition: 'all 0.2s ease'
          }}
          onMouseEnter={(e) => {
            e.target.style.background = '#f8f9fa';
            e.target.style.borderColor = '#adb5bd';
          }}
          onMouseLeave={(e) => {
            e.target.style.background = '#fff';
            e.target.style.borderColor = '#b8b8b8';
          }}
        >
          Save Idea
        </button>
      </div>

      {/* Connection Handle - Only the appropriate target handle based on position */}
      <Handle
        type="target"
        position={isFromTopChannel ? Position.Top : Position.Bottom}
        id={`${id}-target-${isFromTopChannel ? 'top' : 'bottom'}`}
        style={{
          visibility: 'hidden',
          width: '10px',
          height: '10px'
        }}
      />
    </div>
  );
};

export default IdeaNode; 