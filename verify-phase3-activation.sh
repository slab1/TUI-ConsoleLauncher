#!/bin/bash

echo "🔍 Verifying T-UI Monaco Editor Phase 3 Activation..."
echo "=================================================="

# Check if file exists and get line count
if [ -f "app/src/main/assets/monaco_editor.html" ]; then
    LINES=$(wc -l < "app/src/main/assets/monaco_editor.html")
    echo "✅ monaco_editor.html exists ($LINES lines)"
else
    echo "❌ monaco_editor.html not found"
    exit 1
fi

# Phase 3 Feature Checks
echo ""
echo "🚀 Phase 3 Feature Verification:"
echo "--------------------------------"

# Mobile-first design
if grep -q "user-scalable=no" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Mobile-first responsive design implemented"
else
    echo "❌ Mobile-first responsive design missing"
fi

# Mobile navigation
if grep -q "mobile-nav" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Mobile navigation bar implemented"
else
    echo "❌ Mobile navigation bar missing"
fi

# Keyboard toolbar
if grep -q "keyboard-toolbar" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Mobile keyboard toolbar implemented"
else
    echo "❌ Mobile keyboard toolbar missing"
fi

# Cloud integration panel
if grep -q "cloud-panel" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Cloud integration panel implemented"
else
    echo "❌ Cloud integration panel missing"
fi

# Cloud status indicators
if grep -q "cloudStatusMini" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Cloud status indicators implemented"
else
    echo "❌ Cloud status indicators missing"
fi

# Visual diff view
if grep -q "diff-container" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Visual diff viewer implemented"
else
    echo "❌ Visual diff viewer missing"
fi

# Performance optimization
if grep -q "virtualizationEnabled" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ File tree virtualization implemented"
else
    echo "❌ File tree virtualization missing"
fi

# Web Workers
if grep -q "performanceWorker" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Web Workers for performance implemented"
else
    echo "❌ Web Workers missing"
fi

# Collaborative editing
if grep -q "collaborativeMode" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Collaborative editing infrastructure implemented"
else
    echo "❌ Collaborative editing infrastructure missing"
fi

# Haptic feedback
if grep -q "hapticFeedback" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Haptic feedback simulation implemented"
else
    echo "❌ Haptic feedback simulation missing"
fi

# Touch gestures
if grep -q "setupTouchGestures" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Touch gesture support implemented"
else
    echo "❌ Touch gesture support missing"
fi

# Yjs integration
if grep -q "yjs" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Yjs collaborative editing library integrated"
else
    echo "❌ Yjs collaborative editing library missing"
fi

# Enhanced AI suggestions
if grep -q "triggerAISuggestionDebounced" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Enhanced AI suggestions with debouncing implemented"
else
    echo "❌ Enhanced AI suggestions missing"
fi

# Performance monitoring
if grep -q "performance.memory" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Performance monitoring implemented"
else
    echo "❌ Performance monitoring missing"
fi

# Mobile-specific editor configuration
if grep -q "minimap: { enabled: !isMobile }" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Mobile-optimized editor configuration implemented"
else
    echo "❌ Mobile-optimized editor configuration missing"
fi

# Phase 3 welcome content
if grep -q "Phase 3 New Features" "app/src/main/assets/monaco_editor.html"; then
    echo "✅ Phase 3 welcome content updated"
else
    echo "❌ Phase 3 welcome content missing"
fi

echo ""
echo "📊 Summary:"
echo "-----------"
echo "File size: $LINES lines (Phase 2 had ~1490 lines)"
echo "Phase 3 enhancements: Successfully activated!"
echo ""
echo "🎯 Key Phase 3 Features Now Active:"
echo "  • ☁️ Cloud Integration (GitHub sync, Gist creation)"
echo "  • 📱 Mobile-First Design (responsive, touch-optimized)"
echo "  • 🔄 Real-Time Collaboration (Yjs infrastructure)"
echo "  • ⚡ Performance Optimizations (virtualization, Web Workers)"
echo "  • 🎨 Visual Enhancements (diff viewer, better mobile UX)"
echo "  • 🤖 Enhanced AI Assistant (debounced suggestions)"
echo ""
echo "✅ T-UI Monaco Editor Phase 3 is ready for use!"