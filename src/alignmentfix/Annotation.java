/*
 * The MIT License
 *
 * Copyright 2016 ruman.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package alignmentfix;

/**
 *
 * @author ruman
 */
public class Annotation
{
    
    private String id;
    private int extendLeft;
    private int extendRight;
    private int visualPosition;
    private String currentSequence = "";

    public Annotation(String id)
    {
        this.id = id;
    }

    /**
     * @return the extendLeft
     */
    public int getExtendLeft()
    {
        return extendLeft;
    }

    /**
     * @param extendLeft the extendLeft to set
     */
    public void setExtendLeft(int extendLeft)
    {
        this.extendLeft = extendLeft;
    }

    /**
     * @return the extendRight
     */
    public int getExtendRight()
    {
        return extendRight;
    }

    /**
     * @param extendRight the extendRight to set
     */
    public void setExtendRight(int extendRight)
    {
        this.extendRight = extendRight;
    }

    /**
     * @return the visualPosition
     */
    public int getVisualPosition()
    {
        return visualPosition;
    }

    /**
     * @param visualPosition the visualPosition to set
     */
    public void setVisualPosition(int visualPosition)
    {
        this.visualPosition = visualPosition;
    }

    /**
     * @return the id
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the currentSequence
     */
    public String getCurrentSequence()
    {
        return currentSequence;
    }

    /**
     * @param currentSequence the currentSequence to set
     */
    public void setCurrentSequence(String currentSequence)
    {
        this.currentSequence = currentSequence;
    }

    /**
     * Returns true if left/right extensions don't match with the sequence
     * @return
     */
    public boolean isInsane()
    {
        return currentSequence.length() + extendLeft + extendRight <= 0;
    }

    public boolean changed()
    {
        return extendLeft != 0 || extendRight != 0 || visualPosition != 0;
    }
    
    public void clear()
    {
        extendLeft = 0;
        extendRight = 0;
        visualPosition = 0;
    }
}
