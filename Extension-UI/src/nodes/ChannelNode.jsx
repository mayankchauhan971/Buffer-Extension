import React from 'react';
import { Handle, Position, useReactFlow } from '@xyflow/react';

// Define consistent center position for all nodes
const DEFAULT_CENTER_POSITION = {
  x: -100,  // Horizontal center
  y: -150  // Slightly above screen center for better visual appearance
};

const ChannelNode = ({ data, id }) => {
  const { socialMediaChannel, isExpanded, onToggle } = data;
  const { setCenter, getZoom, getNode } = useReactFlow();
  
  const handleClick = (e) => {
    e.stopPropagation();
    
    // Center this node at the consistent default position
    const currentZoom = getZoom();
    
    setCenter(
      DEFAULT_CENTER_POSITION.x,
      DEFAULT_CENTER_POSITION.y,
      { 
        duration: 600,
        zoom: currentZoom // Explicitly preserve current zoom
      }
    );
    
    // Then toggle the socialMediaChannel
    if (onToggle) {
      onToggle(socialMediaChannel);
    }
  };

  return (
    <div 
      onClick={handleClick}
      style={{
        background: isExpanded 
          ? 'linear-gradient(135deg, #2c4bff 0%, #1a3bcc 100%)' 
          : 'rgba(44, 75, 255, 0.1)',
        color: isExpanded ? 'white' : '#2c4bff',
        border: `2px solid ${isExpanded ? '#1a3bcc' : '#2c4bff'}`,
        borderRadius: '12px',
        padding: '12px 16px',
        textAlign: 'center',
        cursor: 'pointer',
        fontWeight: '700',
        fontSize: '14px',
        minWidth: '100px',
        boxShadow: isExpanded 
          ? '0 4px 12px rgba(44, 75, 255, 0.4)' 
          : '0 2px 8px rgba(44, 75, 255, 0.2)',
        transition: 'all 0.3s ease',
        userSelect: 'none',
        position: 'relative',
        transform: isExpanded ? 'scale(1.05)' : 'scale(1)',
      }}
      onMouseEnter={(e) => {
        if (!isExpanded) {
          e.target.style.background = 'rgba(44, 75, 255, 0.2)';
          e.target.style.transform = 'scale(1.02)';
        } else {
          e.target.style.transform = 'scale(1.08)';
        }
      }}
      onMouseLeave={(e) => {
        if (!isExpanded) {
          e.target.style.background = 'rgba(44, 75, 255, 0.1)';
          e.target.style.transform = 'scale(1)';
        } else {
          e.target.style.transform = 'scale(1.05)';
        }
      }}
    >
      {/* Channel Icon and Name */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px'
      }}>
        <span>{socialMediaChannel.charAt(0).toUpperCase() + socialMediaChannel.slice(1)}</span>
        {/* <span style={{ 
          fontSize: '12px',
          transition: 'transform 0.2s ease',
          transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)'
        }}>
          ▶
        </span> */}
      </div>

      {/* Status Indicator */}
      {isExpanded && (
        <div style={{
          position: 'absolute',
          top: '-4px',
          right: '-4px',
          width: '12px',
          height: '12px',
          background: '#28a745',
          borderRadius: '50%',
          border: '2px solid white',
          animation: 'pulse 2s infinite'
        }} />
      )}

      {/* Connection Handles - Only top and bottom with specific IDs */}
      <Handle
        type="target"
        position={Position.Top}
        id={`${socialMediaChannel}-target-top`}
        style={{
          visibility: 'hidden',
          width: '10px',
          height: '10px'
        }}
      />
      <Handle
        type="target"
        position={Position.Bottom}
        id={`${socialMediaChannel}-target-bottom`}
        style={{
          visibility: 'hidden',
          width: '10px',
          height: '10px'
        }}
      />

      {/* Source handles for when expanded - for outgoing edges to ideas */}
      {isExpanded && (
        <>
          <Handle
            type="source"
            position={Position.Top}
            id={`${socialMediaChannel}-source-top`}
            style={{
              visibility: 'hidden',
              width: '10px',
              height: '10px'
            }}
          />
          <Handle
            type="source"
            position={Position.Bottom}
            id={`${socialMediaChannel}-source-bottom`}
            style={{
              visibility: 'hidden',
              width: '10px',
              height: '10px'
            }}
          />
        </>
      )}

      <style jsx>{`
        @keyframes pulse {
          0% {
            box-shadow: 0 0 0 0 rgba(40, 167, 69, 0.7);
          }
          70% {
            box-shadow: 0 0 0 10px rgba(40, 167, 69, 0);
          }
          100% {
            box-shadow: 0 0 0 0 rgba(40, 167, 69, 0);
          }
        }
      `}</style>
    </div>
  );
};

export default ChannelNode; 