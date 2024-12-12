//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.vorono4ka.swf.movieclips;

import com.supercell.swf.FBMovieClipFrame;
import com.supercell.swf.FBResources;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.Savable;
import com.vorono4ka.swf.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MovieClipFrame implements Savable {
    private Tag tag;
    private int elementCount;
    private String label;
    private List<MovieClipFrameElement> elements;

    public MovieClipFrame() {
    }

    public MovieClipFrame(FBMovieClipFrame fb, FBResources resources, int offset) {
        this.label = resources.strings(fb.labelRefId());
        this.elements = new ArrayList(fb.frameElementCount());

        for(int i = 0; i < fb.frameElementCount(); ++i) {
            this.elements.add(new MovieClipFrameElement(resources.movieClipFrameElements(offset + i)));
        }

        this.tag = Tag.MOVIE_CLIP_FRAME_2;
    }

    public int load(ByteStream stream, Tag tag) {
        this.tag = tag;
        int elementCount = stream.readShort();
        this.label = stream.readAscii();
        if (tag == Tag.MOVIE_CLIP_FRAME) {
            this.elements = new ArrayList(elementCount);

            for(int i = 0; i < elementCount; ++i) {
                int childIndex = stream.readShort() & '\uffff';
                int matrixIndex = stream.readShort() & '\uffff';
                int colorTransformIndex = stream.readShort() & '\uffff';
                this.elements.add(new MovieClipFrameElement(childIndex, matrixIndex, colorTransformIndex));
            }
        }

        return elementCount;
    }

    public void save(ByteStream stream) {
        stream.writeShort(this.elements.size());
        stream.writeAscii(this.label);
        if (this.tag == Tag.MOVIE_CLIP_FRAME) {
            Iterator var2 = this.elements.iterator();

            while(var2.hasNext()) {
                MovieClipFrameElement element = (MovieClipFrameElement)var2.next();
                stream.writeShort(element.childIndex());
                stream.writeShort(element.matrixIndex());
                stream.writeShort(element.colorTransformIndex());
            }
        }

    }

    public Tag getTag() {
        return this.tag;
    }

    public String getLabel() {
        return this.label;
    }

    public int getElementCount() {
        return this.elements.size();
    }

    public List<MovieClipFrameElement> getElements() {
        return (this.elements);
    }

    public void setElements(List<MovieClipFrameElement> elements) {
        this.elements = elements;
    }
}
