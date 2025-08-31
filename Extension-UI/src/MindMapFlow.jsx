import React, { useState, useMemo, useCallback, useEffect, useRef } from 'react';
import {
  ReactFlow,
  ReactFlowProvider,
  Background,
  Controls,
  useReactFlow,
  useNodesState,
  useEdgesState,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

import SummaryNode from './nodes/SummaryNode';
import ChannelNode from './nodes/ChannelNode';
import IdeaNode from './nodes/IdeaNode';

const nodeTypes = {
  summary: SummaryNode,
  socialMediaChannel: ChannelNode,
  contentIdea: IdeaNode,
};

const MindMapFlowContent = ({ data }) => {
  const [expandedChannels, setExpandedChannels] = useState(new Set());
  const [containerDimensions, setContainerDimensions] = useState({ width: 0, height: 0 });
  const [summaryHeight, setSummaryHeight] = useState(200);
  const containerRef = useRef(null);
  const reactFlowInstance = useReactFlow();
  
  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current) {
        const { offsetWidth, offsetHeight } = containerRef.current;
        setContainerDimensions({ width: offsetWidth, height: offsetHeight });
      }
    };

    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  const handleSummaryResize = useCallback((height) => {
    setSummaryHeight(height);
  }, []);
  
  /**
   * Toggle socialMediaChannel expansion - only one socialMediaChannel can be expanded at a time
   */
  const toggleChannel = useCallback((socialMediaChannel) => {
    setExpandedChannels(prev => {
      const newSet = new Set();
      
      if (!prev.has(socialMediaChannel)) {
        newSet.add(socialMediaChannel);
        
        // Center on the expanded socialMediaChannel
        setTimeout(() => {
          if (reactFlowInstance) {
            const channelNode = reactFlowInstance.getNode(`socialMediaChannel-${socialMediaChannel}`);
            if (channelNode) {
              const currentZoom = reactFlowInstance.getZoom();
              const centerX = channelNode.position.x + 70;
              const centerY = channelNode.position.y + 25;
              
              reactFlowInstance.setCenter(
                centerX,
                centerY,
                { 
                  duration: 800,
                  zoom: currentZoom
                }
              );
            }
          }
        }, 100);
      } else {
        // When collapsing socialMediaChannel, fit the entire view
        setTimeout(() => {
          if (reactFlowInstance) {
            reactFlowInstance.fitView({ 
              padding: 0.3,
              includeHiddenNodes: false,
              duration: 800,
              minZoom: 0.7,
              maxZoom: 1.2
            });
          }
        }, 100);
      }
      
      return newSet;
    });
  }, [reactFlowInstance]);

  /**
   * Generate nodes and edges based on data and expanded state
   */
  const { nodes, edges } = useMemo(() => {
    const nodes = [];
    const edges = [];
    
    if (!data || !data.channels) {
      return { nodes, edges };
    }

    const channels = Object.keys(data.channels);
    
    const summaryWidth = containerDimensions.width * 0.8;
    const containerWidth = containerDimensions.width || 800;
    const containerHeight = containerDimensions.height || 600;
    
    const summaryPosition = { x: -containerWidth*0.3, y: containerHeight };
    const channelGap = 20;
    const channelsY = summaryPosition.y + summaryHeight + channelGap;
    
    // Summary node
    nodes.push({
      id: 'summary',
      type: 'summary',
      position: summaryPosition,
      data: { 
        summary: data.summary || 'No summary available',
        width: summaryWidth,
        onSizeChange: handleSummaryResize
      },
      draggable: true,
    });
    
    // Channel nodes
    channels.forEach((socialMediaChannel, channelIndex) => {
      const isExpanded = expandedChannels.has(socialMediaChannel);
      
      const channelSpacing = 200;
      const channelPosition = {
        x: (channelIndex - (channels.length - 1) / 2) * channelSpacing,
        y: channelsY
      };

      nodes.push({
        id: `socialMediaChannel-${socialMediaChannel}`,
        type: 'socialMediaChannel',
        position: channelPosition,
        data: {
          socialMediaChannel,
          isExpanded: isExpanded,
          onToggle: toggleChannel,
        },
        draggable: true,
        key: `${socialMediaChannel}-${isExpanded}-${channelPosition.x}-${channelPosition.y}`,
      });

      edges.push({
        id: `summary-to-${socialMediaChannel}`,
        source: 'summary',
        target: `socialMediaChannel-${socialMediaChannel}`,
        sourceHandle: 'summary-bottom',
        targetHandle: `${socialMediaChannel}-target-top`,
        label: `${data.channels[socialMediaChannel]?.length || 0} ideas`,
        style: { stroke: '#2c4bff', strokeWidth: 2 },
      });

      // Add contentIdea nodes if socialMediaChannel is expanded
      if (isExpanded) {
        const channelIdeas = data.channels[socialMediaChannel] || [];
        const ideasToShow = channelIdeas.slice(0, 3);
        
        ideasToShow.forEach((contentIdea, ideaIndex) => {
          const ideaId = `contentIdea-${socialMediaChannel}-${ideaIndex}`;
          
          const ideaSpacing = 350;
          const totalWidth = (ideasToShow.length - 1) * ideaSpacing;
          const startX = channelPosition.x - (totalWidth / 2);
          
          const ideaPosition = {
            x: startX + ideaIndex * ideaSpacing,
            y: channelPosition.y + 200
          };

          nodes.push({
            id: ideaId,
            type: 'contentIdea',
            position: ideaPosition,
            data: {
              contentIdea,
              ideaIndex,
              socialMediaChannel,
              isFromTopChannel: true,
            },
            draggable: true,
          });

          edges.push({
            id: `${socialMediaChannel}-to-${ideaId}`,
            source: `socialMediaChannel-${socialMediaChannel}`,
            target: ideaId,
            sourceHandle: `${socialMediaChannel}-source-bottom`,
            targetHandle: `${ideaId}-target-top`,
            label: `Idea ${ideaIndex + 1}`,
            style: { stroke: '#667eea', strokeWidth: 2 },
          });
        });
      }
    });

    return { nodes, edges };
  }, [data, expandedChannels, toggleChannel, containerDimensions, summaryHeight, handleSummaryResize]);

  const [nodesState, setNodes, onNodesChange] = useNodesState(nodes);
  const [edgesState, setEdges, onEdgesChange] = useEdgesState(edges);

  React.useEffect(() => {
    try {
      setNodes(nodes);
      setEdges(edges);
    } catch (error) {
      console.error('Error updating nodes/edges:', error);
    }
  }, [nodes, edges, setNodes, setEdges]);

  const onInit = useCallback((reactFlowInstance) => {
    setTimeout(() => {
      try {
        reactFlowInstance.fitView({ 
          padding: 0.3,
          includeHiddenNodes: false,
          duration: 500,
          minZoom: 0.7,
          maxZoom: 1.2
        });
      } catch (error) {
        console.error('Error in onInit fitView:', error);
      }
    }, 300);
  }, []);

  return (
    <div ref={containerRef} style={{ 
      width: '95%', 
      height: '95%', 
      margin: '2.5%', 
      position: 'relative',
      overflow: 'hidden'
    }}>
      <ReactFlow
        nodes={nodesState}
        edges={edgesState}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        onInit={onInit}
        fitView
        snapToGrid={true}
        snapGrid={[20, 20]}
        attributionPosition="bottom-left"
        defaultViewport={{ x: 0, y: 0, zoom: 0.7 }}
        minZoom={0.5}
        maxZoom={2}
        nodesDraggable={true}
        nodesConnectable={false}
        elementsSelectable={true}
        panOnDrag={true}
        selectNodesOnDrag={false}
        edgesFocusable={false}
        edgesReconnectable={false}
        nodesFocusable={false}
        draggable={false}
      >
        <Background 
          color="#e1e5e9" 
          gap={20} 
          size={1.5}
          variant="dots"
          style={{ opacity: 0.4 }}
        />
        
        <Controls 
          showZoom={true}
          showFitView={true}
          showInteractive={false}
          position="bottom-left"
          style={{
            background: 'rgba(255, 255, 255, 0.9)',
            borderRadius: '8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
          }}
        />
      </ReactFlow>
    </div>
  );
};

const MindMapFlow = ({ data }) => {
  return (
    <ReactFlowProvider>
      <MindMapFlowContent data={data} />
    </ReactFlowProvider>
  );
};

export default MindMapFlow; 