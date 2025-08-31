const path = require('path');

module.exports = {
  // Single entry point for sidepanel brainstorm functionality
  // This builds the React components for the Chrome extension sidepanel
  entry: {
    'sidepanel-brainstorm': './src/sidepanel-brainstorm.js'
  },
  
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: '[name].bundle.js',
    clean: true // Clean dist folder on each build
  },
  
  module: {
    rules: [
      {
        // Process React JSX files
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-react']
          }
        }
      },
      {
        // Process CSS files (for ReactFlow styles)
        test: /\.css$/,
        use: ['style-loader', 'css-loader']
      }
    ]
  },
  
  resolve: {
    extensions: ['.js', '.jsx']
  },
  
  optimization: {
    // Split vendor dependencies (React, ReactFlow) into separate bundle
    // This improves caching and reduces main bundle size
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all',
        },
      },
    },
  }
}; 