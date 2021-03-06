package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polygon;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.model.FeedbackLoop;
import streamit.eclipse.debugger.model.Filter;
import streamit.eclipse.debugger.model.Pipeline;
import streamit.eclipse.debugger.model.SplitJoin;

/**
 * @author kkuo
 */
public class SplitJoinWidget extends Polygon implements IStreamStructureWidget {
    private SplitJoin fModel;
    private int fTallestChild, fChildWidth;
    private boolean fExpanded;
    private boolean fStretchTopChannel, fStretchBottomChannel;
    
    // widgets  
    private Label fHeader;
    private ImageFigure fArrow;
    private ChannelWidget fTopChannel;
    private ChannelWidget fBottomChannel;
    private Vector fChildren;
    private Polygon fSplitter;
    private Polygon fJoiner;
    private Vector fSplitterWeights;    // Vector of Figures
    private Vector fJoinerWeights;      // Vector of Figures
    
    public SplitJoinWidget(SplitJoin model, String streamNameWithId, OptionData optionData, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
        super();
        fModel = model;
        fStretchTopChannel = false;
        fStretchBottomChannel = false;
    
        // create splitjoin
        new StreamStructureSelector(this);
        String splitjoinName = getNameWithRuntimeId();
        fExpanded = optionData.containsStream(splitjoinName, false);
        fHeader = new Label(splitjoinName);
        Dimension splitjoinSize = FigureUtilities.getTextExtents(splitjoinName, factoryInst.getFont());
        
        // splitjoin style
        if (optionData.isHideLines() && fExpanded) setOutline(false);
        else setOutline(true);
        setForegroundColor(ColorConstants.menuForeground);
        setBackgroundColor(ColorConstants.white);

        // possible highlight
        if (streamNameWithId.equals(splitjoinName)) StreamStructureSelector.setSelection(this);
        
        if (!fExpanded) {

            // collapsed content
            fHeader.setIcon(factoryInst.getPlus());
            splitjoinSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
            factoryInst.roundUpEven(splitjoinSize);
            fHeader.setSize(splitjoinSize);
            add(fHeader);
            
            // collapsed size
            splitjoinSize.expand(IStreamItGraphConstants.MARGIN, factoryInst.getArrowHeight()*2 + IStreamItGraphConstants.MARGIN);
        } else {
            
            // expanded header
            fHeader.setIcon(factoryInst.getMinus());
            splitjoinSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
            factoryInst.roundUpEven(splitjoinSize);
            fHeader.setSize(splitjoinSize);
            add(fHeader);
            
            // expanded splitter
            fSplitter = new Polygon();
            add(fSplitter);
            
            // splitter weights
            fSplitterWeights = new Vector();
            factoryInst.createWeights(fModel.getWeights(true), true && fModel.isDuplicateSplitter(), fSplitterWeights, this);

            // expanded children
            Vector children = fModel.getChildStreams();
            Dimension childrenSize = new Dimension(IStreamItGraphConstants.MARGIN/2, 0); // (total width of children, height of tallest child)
            fChildren = new Vector();
            Object child;
            for (int i = 0; i < children.size(); i++) {
                child = children.get(i);
                if (child instanceof Filter) {
                    fChildren.add(new FilterWidget((Filter) child, streamNameWithId, optionData, this, forward, false, true, childrenSize, factoryInst));
                } else if (child instanceof Pipeline) {
                    fChildren.add(new PipelineWidget((Pipeline) child, streamNameWithId, optionData,  this, forward, false, true, childrenSize, factoryInst));
                } else if (child instanceof SplitJoin) {
                    fChildren.add(new SplitJoinWidget((SplitJoin) child, streamNameWithId, optionData, this, forward, false, true, childrenSize, factoryInst)); 
                } else if (child instanceof FeedbackLoop) {
                    fChildren.add(new FeedbackLoopWidget((FeedbackLoop) child, streamNameWithId, optionData, this, forward, false, true, childrenSize, factoryInst));
                }
                childrenSize.width += IStreamItGraphConstants.MARGIN/2;
            }
            int numChildren = fChildren.size();
            if (numChildren == 0) setOutline(true);

            // expanded joiner
            fJoiner = new Polygon();
            add(fJoiner);

            // joiner weights
            fJoinerWeights = new Vector();
            factoryInst.createWeights(fModel.getWeights(false), false, fJoinerWeights, this);

            // expanded size
            fTallestChild = childrenSize.height;
            fChildWidth = childrenSize.width;           
            splitjoinSize.width = Math.max(childrenSize.width, splitjoinSize.width*2 + IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN*2);
            splitjoinSize.height = Math.max(childrenSize.height, splitjoinSize.height) + IStreamItGraphConstants.CHANNEL_WIDTH*2 + splitjoinSize.height*2 + IStreamItGraphConstants.MARGIN*2 + factoryInst.getArrowHeight()*2;
        }
        setSize(splitjoinSize);
        
        // content arrow
        fArrow = new ImageFigure(factoryInst.getArrow(forward));
        fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
        add(fArrow);

        // create channels
        if (forward) {
            fTopChannel = new ChannelWidget(fModel.getInputChannel(), splitjoinName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
            fBottomChannel = new ChannelWidget(fModel.getOutputChannel(), splitjoinName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);
            fTopChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
        } else {
            fBottomChannel = new ChannelWidget(fModel.getInputChannel(), splitjoinName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
            fTopChannel = new ChannelWidget(fModel.getOutputChannel(), splitjoinName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);
            fBottomChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));

            // swap joiner & splitter
            Vector temp = fJoinerWeights;
            fJoinerWeights = fSplitterWeights;
            fSplitterWeights = temp;
        }
        ((IStreamStructureWidget) parent).setChannelExpanded(fTopChannel.isExpanded(), fBottomChannel.isExpanded());

        // collapsed content
        parent.add(this);
            
        if (verticalLayout) {
            // (total height of children, width of widest child)
            parentSize.height = parentSize.height + fTopChannel.getSize().height + splitjoinSize.height;
            if (lastChild) parentSize.height += fBottomChannel.getSize().height;
            parentSize.width = Math.max(parentSize.width, splitjoinSize.width);
        } else {
            // (height of tallest child, total width of children)
            parentSize.height = Math.max(parentSize.height, fTopChannel.getSize().height + splitjoinSize.height + fBottomChannel.getSize().height);
            parentSize.width = parentSize.width + Math.max(splitjoinSize.width, fTopChannel.getSize().width);
        }
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#setLocation(org.eclipse.draw2d.geometry.Point, int)
     */
    public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
        Dimension topSize = fTopChannel.getSize();
        Dimension splitjoinSize = getSize();
        Dimension headerSize = fHeader.getSize();
        Dimension arrowSize = fArrow.getSize();
        Dimension bottomSize = fBottomChannel.getSize();

        if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight), true)) currentHeight += topSize.height;

        Point splitjoinTopCenter = parentTopCenter.getTranslated(0, currentHeight);
        setLocation(parentTopCenter.getTranslated(-splitjoinSize.width/2, currentHeight));
        
        if (!isExpanded()) {
            // collapsed location
            fHeader.setLocation(splitjoinTopCenter.getTranslated(-headerSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
            fArrow.setLocation(splitjoinTopCenter.getTranslated(-arrowSize.width/2, headerSize.height + arrowSize.height + IStreamItGraphConstants.MARGIN/2));
            currentHeight += splitjoinSize.height;

            Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
            if (fBottomChannel.setSelf(bottomLeft, false)) currentHeight += bottomSize.height;
            
            // splitjoin points (8)
            addPoint(splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
            addPoint(splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0));
            addPoint(splitjoinTopCenter.getTranslated(splitjoinSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
            addPoint(bottomLeft.getTranslated(splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height - IStreamItGraphConstants.MARGIN/2));
            addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
            addPoint(bottomLeft.getCopy());
            addPoint(bottomLeft.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height - IStreamItGraphConstants.MARGIN/2));
            addPoint(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
        } else {
            int numChildren = fChildren.size(); 
            
            // expanded location
            if (numChildren > 0) {
                // layout children
                int currentWidth = splitjoinTopCenter.x - fChildWidth/2 + IStreamItGraphConstants.MARGIN/2;
                IStreamStructureWidget child;
                int childHeight = splitjoinTopCenter.y + IStreamItGraphConstants.CHANNEL_WIDTH + headerSize.height + IStreamItGraphConstants.MARGIN + arrowSize.height; 
                for (int i = 0; i < numChildren; i++) {
                    child = (IStreamStructureWidget) fChildren.get(i);
                    currentWidth = child.setHorizontalLocation(childHeight, fStretchTopChannel, fStretchBottomChannel, fTallestChild, currentWidth);
                }
                
                // fSplitter
                Point bottomLeft = ((IStreamStructureWidget) fChildren.get(0)).getTopChannelTopLeft();
                Point bottomRight = ((IStreamStructureWidget) fChildren.get(numChildren - 1)).getTopChannelTopRight();
                Point topLeft = splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0);
                Point topRight = splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0);
                Point middleLeft = new Point(topLeft.x, bottomLeft.y - IStreamItGraphConstants.CHANNEL_WIDTH);
                Point middleRight = new Point(topRight.x, bottomRight.y - IStreamItGraphConstants.CHANNEL_WIDTH);
                
                Point typePoint = bottomLeft.getTranslated(0, -IStreamItGraphConstants.CHANNEL_WIDTH);
                fSplitter.addPoint(bottomLeft.getCopy());
                fSplitter.addPoint(typePoint);
                fSplitter.addPoint(middleLeft);
                fSplitter.addPoint(topLeft);
                fSplitter.addPoint(topRight);
                fSplitter.addPoint(middleRight);
                fSplitter.addPoint(bottomRight.getTranslated(0, -IStreamItGraphConstants.CHANNEL_WIDTH)); 
                fSplitter.addPoint(bottomRight.getCopy());
            }
            
            currentHeight += splitjoinSize.height;
            
            Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
            if (fBottomChannel.setSelf(bottomLeft, false)) currentHeight += bottomSize.height;
            
            if (numChildren > 0) {
                // fJoiner              
                Point topLeft = ((IStreamStructureWidget) fChildren.get(0)).getBottomChannelBottomLeft();
                Point topRight = ((IStreamStructureWidget) fChildren.get(numChildren - 1)).getBottomChannelBottomRight();
                Point bottomRight = bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH - 1, 0);
                Point middleLeft = new Point(bottomLeft.x, topLeft.y + IStreamItGraphConstants.CHANNEL_WIDTH);
                Point middleRight = new Point(bottomRight.x, topRight.y + IStreamItGraphConstants.CHANNEL_WIDTH);
                
                fJoiner.addPoint(topLeft.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
                fJoiner.addPoint(topLeft.getCopy());
                fJoiner.addPoint(topRight.getCopy());
                fJoiner.addPoint(topRight.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
                fJoiner.addPoint(middleRight);
                fJoiner.addPoint(bottomRight);
                fJoiner.addPoint(bottomLeft.getCopy());
                fJoiner.addPoint(middleLeft);
            }

            // set fSplitterWeights, fJoinerWeights
            Point p;
            boolean setSplitterWeights = fSplitterWeights.size() == numChildren;
            boolean setJoinerWeights = fJoinerWeights.size() == numChildren;
            for (int i = 0; i < numChildren; i++) {
                if (setSplitterWeights) {
                    p = ((IStreamStructureWidget) fChildren.get(i)).getTopChannelTopLeft();
                    ((IFigure) fSplitterWeights.get(i)).setLocation(p.getTranslated(IStreamItGraphConstants.MARGIN, -IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN));
                }
                if (setJoinerWeights) {
                    p = ((IStreamStructureWidget) fChildren.get(i)).getBottomChannelBottomLeft();
                    ((IFigure) fJoinerWeights.get(i)).setLocation(p.getTranslated(IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN));
                }
            }

            // splitjoin points (8)
            addPoint(splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
            addPoint(splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));         
            addPoint(splitjoinTopCenter.getTranslated(splitjoinSize.width/2, arrowSize.height));
            addPoint(bottomLeft.getTranslated(splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height));
            addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
            addPoint(bottomLeft.getCopy());
            addPoint(bottomLeft.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height));
            addPoint(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2, arrowSize.height));

            // fHeader
            fHeader.setLocation(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.MARGIN/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
            fArrow.setLocation(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.MARGIN/2, headerSize.height + arrowSize.height + IStreamItGraphConstants.MARGIN/2));
        }
        
        return currentHeight;
    }
    
    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
     */
    public int setHorizontalLocation(int currentHeight, boolean stretchTopChannel, boolean stretchBottomChannel, int stretchHeight, int currentWidth) {
        Dimension topSize = fTopChannel.getSize();
        Dimension splitjoinSize = getSize();
        Dimension bottomSize = fBottomChannel.getSize();

        // expand channels
        if (stretchTopChannel) {
            topSize.height = stretchHeight - splitjoinSize.height - bottomSize.height;
            fTopChannel.setSize(topSize);
        } 
        if (stretchBottomChannel) {
            bottomSize.height = stretchHeight - splitjoinSize.height - topSize.height;
            fBottomChannel.setSize(bottomSize);
        }

        setVerticalLocation(new Point(currentWidth + splitjoinSize.width/2, currentHeight), 0);
        currentWidth = currentWidth +  splitjoinSize.width + IStreamItGraphConstants.MARGIN/2;
        return currentWidth;
    }
    
    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getTopChannelTopLeft()
     */
    public Point getTopChannelTopLeft() {
        return fTopChannel.getLocation();
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getTopChannelTopRight()
     */
    public Point getTopChannelTopRight() {
        return fTopChannel.getBounds().getTopRight().getTranslated(-1, 0);
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getBottomChannelBottomLeft()
     */
    public Point getBottomChannelBottomLeft() {
        return fBottomChannel.getBounds().getBottomLeft();
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getBottomChannelBottomRight()
     */
    public Point getBottomChannelBottomRight() {
        return fBottomChannel.getBounds().getBottomRight().getTranslated(-1, 0);
    }
    
    public void setChannelExpanded(boolean top, boolean bottom) {
        if (!fStretchTopChannel && top) fStretchTopChannel = top;
        if (!fStretchBottomChannel && bottom) fStretchBottomChannel = bottom;
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getNameWithId()
     */
    public String getNameWithRuntimeId() {
        return fModel.getNameWithRuntimeId();
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getNameWithoutId()
     */
    public String getNameWithoutId() {
        return fModel.getNameWithoutId();
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getId()
     */
    public String getRuntimeId() {
        return fModel.getRuntimeId();
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStreamStructureWidget#getStaticId()
     */
    public String getStaticId() {
        return fModel.getStaticId();
    }
    
    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#isWithinIcon(org.eclipse.draw2d.geometry.Point)
     */
    public boolean isWithinIcon(Point p) {
        return fHeader.getIconBounds().contains(p);
    }

    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#isExpanded()
     */
    public boolean isExpanded() {
        return fExpanded;
    }
    /* (non-Javadoc)
     * @see streamit.eclipse.debugger.graph.IStream#getTopChannelToggleWidth()
     */
    public int getTopChannelToggleWidth() {
        return fTopChannel.getChannelToggleWidth();
    }
    
    public Vector getChildStreams() {
        return fChildren;
    }
}
