package org.testng.eclipse.util;

import org.testng.eclipse.TestNGPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.xml.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class SuiteFileValidator {
  private static final SAXParserFactory SAX_FACTORY = SAXParserFactory.newInstance();
  private static SAXParser SAXPARSER;
  private static final Pattern SUITE_REGEXP = Pattern.compile("<suite.*");
  private static final Pattern TAG_REGEXP = Pattern.compile("<[^>?!]+>");
  
  /**
   * @return true if the InputStream represents a TestNG definition file.
   * For speed reasons, we stop the search as soon as we find an XML tag.
   * If it's <suite>, return true, otherwise, return false (we also dodge
   * DOCTYPES and other XML niceties with the TAG_REGEXP regular
   * expression defined above.
   */
  public static boolean isSuiteDefinition(InputStream is) {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    
    try {
      String line = br.readLine();
      while (null != line) {
        if (SUITE_REGEXP.matcher(line).matches()) {
          return true;
        }
        else if (TAG_REGEXP.matcher(line).matches()) {
          return false;
        }
        else line = br.readLine();
      }
    }
    catch (IOException e) {
      TestNGPlugin.log(e);
    }
    
    return false;
  }
  
  public static void ppp(String s) {
    System.out.println("[SuiteFileValidator] " + s);
  }
  
  private static SAXParser getParser() {
    if(null == SAXPARSER) {
      try {
        SAX_FACTORY.setValidating(false);
        SAXPARSER = SAX_FACTORY.newSAXParser();
      }
      catch(ParserConfigurationException pce) {
        TestNGPlugin.log(pce);
      }
      catch(SAXException saxe) {
        TestNGPlugin.log(saxe);
      }
    }
    
    return SAXPARSER;
  }
  
  private static class SuiteHandler extends DefaultHandler {
    protected boolean m_isValid = false;
    protected int m_elementCount = 0;
    
    public boolean isValid() {
      return m_isValid;
    }
    
    
  public InputSource resolveEntity(String publicId, String systemId) 
    throws SAXException 
    {
      InputSource result = null;
      if(Parser.TESTNG_DTD_URL.equals(publicId)) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(Parser.TESTNG_DTD);
        if(null == is) {
          is = Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(Parser.TESTNG_DTD);
          if(null == is) {
            System.out.println("WARNING: couldn't find in classpath " + Parser.TESTNG_DTD_URL
                + "\n" + "Fetching it from the Web site.");
            try {
              result = super.resolveEntity(systemId, publicId);
            }
            catch (Exception e) {
              throw new SAXException(e);
            }
          } 
          else {
            result = new InputSource(is);
          }
        } 
        else {
          result = new InputSource(is);
        }
      } 
      else {
        try {
          result = super.resolveEntity(systemId, publicId);
        }
        catch (Exception e) {
          throw new SAXException(e);
        }
      }
      
      return result;
  }


    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if(m_elementCount++ == 0) {
        if ("suite".equals(qName)) {
          m_isValid = true;
        }
      } 
    }
  }
}