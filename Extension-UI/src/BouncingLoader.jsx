import React from 'react';

const BouncingLoader = ({ 
  message = "Buffer-ing", 
  subMessage = "Extracting page content...", 
  size = "medium",
  color = "#2c4bff" 
}) => {
  const sizeStyles = {
    small: {
      fontSize: '14px',
      subFontSize: '10px',
      dotSize: '8px',
      spacing: '16px'
    },
    medium: {
      fontSize: '18px', 
      subFontSize: '12px',
      dotSize: '10px',
      spacing: '20px'
    },
    large: {
      fontSize: '24px',
      subFontSize: '14px', 
      dotSize: '12px',
      spacing: '24px'
    }
  };

  const currentSize = sizeStyles[size] || sizeStyles.medium;

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100%',
      fontFamily: "'Roboto', sans-serif",
      color: color
    }}>
      <div style={{ 
        fontSize: currentSize.fontSize, 
        fontWeight: '500', 
        marginBottom: currentSize.spacing 
      }}>
        {message}
        <span className="bouncing-dots">
          <span>.</span>
          <span>.</span>
          <span>.</span>
          <span>.</span>
          <span>.</span>
        </span>
      </div>
      {subMessage && (
        <div style={{ 
          fontSize: currentSize.subFontSize, 
          color: '#666' 
        }}>
          {subMessage}
        </div>
      )}
      
      <style>
        {`@keyframes bouncing-bounce {
          0%, 80%, 100% { 
            transform: translateY(0); 
            opacity: 0.3; 
          }
          40% { 
            transform: translateY(-${currentSize.dotSize}px); 
            opacity: 1; 
          }
        }
        
        .bouncing-dots span {
          animation: bouncing-bounce 1.4s infinite ease-in-out;
          animation-fill-mode: both;
          display: inline-block;
          margin-left: 1px;
        }
        
        .bouncing-dots span:nth-child(1) { animation-delay: -0.32s; }
        .bouncing-dots span:nth-child(2) { animation-delay: -0.16s; }
        .bouncing-dots span:nth-child(3) { animation-delay: 0s; }
        .bouncing-dots span:nth-child(4) { animation-delay: 0.16s; }
        .bouncing-dots span:nth-child(5) { animation-delay: 0.32s; }`}
      </style>
    </div>
  );
};

export default BouncingLoader; 