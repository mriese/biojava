/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */

package org.biojava.bio.program.tagvalue;

import org.biojava.utils.ParserException;

/**
 * <p>
 * An object that wishes to be informed of events during the parsing of a file.
 * </p>
 *
 * <p>
 * This interface is similar in spirit to the SAX interfaces for parsing XML.
 * Many of the methods will always be called in appropriately nested pairs.
 * Entire records will be bracketed by a startRecord and endRecord pair. Within
 * these, any number of startTag and endTag pairs may be called. Within a
 * tag pair, any number of value invocations may be called. If a value is
 * complex and requires parsing as a sub-entry, then the TagValueContext
 * interface can be used to push a new TagValueParser and listener pair onto the parser
 * stack. This will result in the pushed listener recieving a start/end document
 * notification encapsulating the entire sub-set of events generated by the
 * parser using the pushed TagValueParser to process the sub-document.
 * </p>
 *
 * @author Matthew Pocock
 * @since 1.2
 */
public interface TagValueListener {
  /**
   * A new record is about to start.
   *
   * @throws ParserException if the record can not be started
   */
  public void startRecord()
  throws ParserException;
  
  /**
   * The current record has ended.
   *
   * @throws ParserException if the record can not be ended
   */
  public void endRecord()
  throws ParserException;
  
  /**
   * Start a new tag.
   *
   * @param tag the Object representing the new tag
   * @throws ParserException if the tag could not be started
   */
  public void startTag(Object tag)
  throws ParserException;
  
  /**
   * End the current tag.
   *
   * @throws ParserException if the tag could not be ended
   */
  public void endTag()
  throws ParserException;
  
  /**
   * A value has been seen.
   *
   * @param ctxt a TagValueContext that could be used to push a sub-document
   * @param value  the value Object observed
   * @throws ParserException if the value could not be processed
   */
  public void value(TagValueContext ctxt, Object value)
  throws ParserException;
}