import React, { useEffect } from 'react';
import { Handle, Position, useReactFlow } from '@xyflow/react';

const SummaryNode = ({ data, id }) => {
  const summary = data.summary || 'No summary available';
  const nodeWidth = data.width || 300;
  const onSizeChange = data.onSizeChange;
  const { setCenter, getZoom } = useReactFlow();

  const handleClick = (e) => {
    e.stopPropagation();
    const { fitView } = useReactFlow();
    
    fitView({ 
      padding: 0.3,
      includeHiddenNodes: false,
      duration: 600,
      minZoom: 0.5,
      maxZoom: 1.2
    });
  };
  
  // Calculate node height based on content
  const baseHeight = 90;
  const lineHeight = 24;
  const charsPerLine = Math.floor((nodeWidth - 32) / 10);
  const lines = Math.ceil(summary.length / charsPerLine);
  const contentHeight = lines * lineHeight;
  const totalHeight = baseHeight + contentHeight;

  useEffect(() => {
    if (onSizeChange) {
      onSizeChange(totalHeight);
    }
  }, [totalHeight, onSizeChange]);

  return (
    <div 
      onClick={handleClick}
      style={{
        cursor: 'pointer',
        background: '#ffffff',
        border: '2px solid #e9ecef',
        borderRadius: '12px',
        padding: '16px',
        fontSize: '14px',
        position: 'relative',
        transition: 'all 0.2s ease',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column'
      }}
      onMouseEnter={(e) => {
        e.target.style.transform = 'scale(1.02)';
        e.target.style.borderColor = '#667eea';
      }}
      onMouseLeave={(e) => {
        e.target.style.transform = 'scale(1)';
        e.target.style.borderColor = '#e9ecef';
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
        marginBottom: '16px',
        flexShrink: 0
      }}>
        📋 Webpage Summary
      </div>

      {/* Summary Content */}
      <div style={{
        fontSize: '14px',
        lineHeight: '1.5',
        color: '#333',
        flex: 1,
        overflow: 'auto'
      }}>
        {summary}
      </div>

      {/* Connection handles for React Flow */}
      <Handle
        type="source"
        position={Position.Top}
        id="summary-top"
        style={{
          visibility: 'hidden',
          width: '12px',
          height: '12px'
        }}
      />
      <Handle
        type="source"
        position={Position.Bottom}
        id="summary-bottom"
        style={{
          visibility: 'hidden',
          width: '12px',
          height: '12px'
        }}
      />
    </div>
  );
};

export default SummaryNode; 