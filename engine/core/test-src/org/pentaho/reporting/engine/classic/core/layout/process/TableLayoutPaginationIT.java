/*
 * This program is free software; you can redistribute it and/or modify it under the
 *  terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 *  Foundation.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this
 *  program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  or from the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  Copyright (c) 2006 - 2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.engine.classic.core.layout.process;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.layout.model.LogicalPageBox;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderNode;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderableText;
import org.pentaho.reporting.engine.classic.core.testsupport.DebugReportRunner;
import org.pentaho.reporting.engine.classic.core.testsupport.selector.MatchFactory;
import org.pentaho.reporting.engine.classic.core.util.geom.StrictGeomUtility;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * These are several tests related to pagination for table layout. Each used file represent a separate case.<br/>
 * All tests have same elements' properties:
 * <ul>
 *   <li>Page setup - standard portrait-oriented {@linkplain
 * org.pentaho.reporting.engine.classic.core.util.PageSize#LETTER LETTER} with margins of 72; the overall free vertical
 * space is (792 - 72 x 2) = 648</li>
 *   <li>Each row's height is 200</li>
 *   <li>Table header's and footer's height are 150</li>
 * </ul>
 *
 * By default, each test's dataset contains 10 elements, with names defining their order number (1..10).
 *
 * @author Andrey Khayrutdinov
 */
public class TableLayoutPaginationIT {

  private static final String PAGE_HEADER_NAME = "page-header";
  private static final String PAGE_HEADER_VALUE = "Page-Header";

  private static final String PAGE_FOOTER_NAME = "page-footer";
  private static final String PAGE_FOOTER_VALUE = "Page-Footer";

  private static final String HEADER_NAME = "header";
  private static final int HEADER_HEIGHT = 150;
  private static final String HEADER_VALUE = "Header";

  private static final String FOOTER_NAME = "footer";
  private static final int FOOTER_HEIGHT = 150;
  private static final String FOOTER_VALUE = "Footer";

  private static final String DETAIL_ROW_NAME = "detailRow";
  private static final int ROW_HEIGHT = 200;


  @BeforeClass
  public static void init() throws Exception {
    ClassicEngineBoot.getInstance().start();
  }

  private static List<LogicalPageBox> loadPages( String file, int expectedPages ) throws Exception {
    ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();
    Resource resource =
      resourceManager.createDirectly( TableLayoutPaginationIT.class.getResource( file ), MasterReport.class );
    MasterReport report = (MasterReport) resource.getResource();

    int[] pages = new int[ expectedPages ];
    for ( int i = 0; i < expectedPages; i++ ) {
      pages[ i ] = i;
    }
    return DebugReportRunner.layoutPagesStrict( report, expectedPages, pages );
  }


  /*
   * No header and no footer
   *
   * Expected layout:
   *  1: 1,2,3
   *  2: 4,5,6
   *  3: 7,8,9
   *  4: 10
   */
  @Test
  public void paginate_NoHeader_NoFooter() throws Exception {
    String file = "table-layout-pagination-no-header-no-footer.prpt";
    List<LogicalPageBox> pages = loadPages( file, 4 );

    PageValidator validator = validator().build();
    validator.validatePage( pages.get( 0 ), "1", "2", "3" );
    validator.validatePage( pages.get( 1 ), "4", "5", "6" );
    validator.validatePage( pages.get( 2 ), "7", "8", "9" );
    validator.validatePage( pages.get( 3 ), "10" );
  }

  /*
   * A report with header. By design, header is displayed on each page for table layout
   *
   * Expected layout:
   *  1: h,1,2
   *  ........
   *  5: h,9,10
   */
  @Test
  public void paginate_WithHeader() throws Exception {
    String file = "table-layout-pagination-header-no-footer.prpt";
    List<LogicalPageBox> pages = loadPages( file, 5 );

    PageValidator validator = validator().withTableHeader().build();
    validator.validatePage( pages.get( 0 ), "1", "2" );
    validator.validatePage( pages.get( 1 ), "3", "4" );
    validator.validatePage( pages.get( 2 ), "5", "6" );
    validator.validatePage( pages.get( 3 ), "7", "8" );
    validator.validatePage( pages.get( 4 ), "9", "10" );
  }

  /*
   * A report with footer. It should be placed in the end of the report
   *
   * Expected layout:
   *  1: 1,2,3
   *  ........
   *  3: 7,8,9
   *  4: 10,f
   */
  @Test
  public void paginate_WithFooter() throws Exception {
    String file = "table-layout-pagination-no-header-footer.prpt";
    List<LogicalPageBox> pages = loadPages( file, 4 );

    PageValidator validator = validator().build();
    validator.validatePage( pages.get( 0 ), "1", "2", "3" );
    validator.validatePage( pages.get( 1 ), "4", "5", "6" );
    validator.validatePage( pages.get( 2 ), "7", "8", "9" );
    validator.checkTableFooter().validatePage( pages.get( 3 ), "10" );
  }

  /*
   * A report with both header and footer
   *
   * Expected layout:
   *  1: h,1,2
   *  ........
   *  5: h,9,10
   *  6: h,f
   */
  @Test
  public void paginate_WithHeaderAndFooter() throws Exception {
    String file = "table-layout-pagination-header-footer.prpt";
    List<LogicalPageBox> pages = loadPages( file, 6 );

    PageValidator validator = validator().withTableHeader().build();
    validator.validatePage( pages.get( 0 ), "1", "2" );
    validator.validatePage( pages.get( 1 ), "3", "4" );
    validator.validatePage( pages.get( 2 ), "5", "6" );
    validator.validatePage( pages.get( 3 ), "7", "8" );
    validator.validatePage( pages.get( 4 ), "9", "10" );
    validator.checkTableFooter().validatePage( pages.get( 5 ) );
  }

  /*
   * A special test with 100 records. It is needed to reveal possible mistakes
   * which amass slowly and are not discernible with 3-4 pages.
   *
   * Expected layout:
   *  01: h,1,2
   *  ............
   *  50: h,99,100
   *  51: h,f
   */
  @Test
  public void paginate_WithHeaderAndFooter_Long() throws Exception {
    String file = "table-layout-pagination-header-footer-100-records.prpt";
    List<LogicalPageBox> pages = loadPages( file, 51 );

    PageValidator validator = validator().withTableHeader().build();
    int recordCounter = 1;
    for ( int i = 0; i < 50; i++ ) {
      String firstRow = Integer.toString( recordCounter++ );
      String secondRow = Integer.toString( recordCounter++ );
      validator.validatePage( pages.get( i ), firstRow, secondRow );
    }
    validator.checkTableFooter().validatePage( pages.get( 50 ) );
  }


  /*
   * A test with a small page header (height = 50). Since it suits 98 gap, the expected layout should be similar to
   * the case the no page header
   */
  @Test
  public void paginate_WithHeaderAndSmallPageHeader() throws Exception {
    String file = "table-layout-pagination-page-header-small.prpt";
    List<LogicalPageBox> pages = loadPages( file, 5 );

    PageValidator validator = validator().withPageHeader( 50 ).withTableHeader().build();
    validator.validatePage( pages.get( 0 ), "1", "2" );
    validator.validatePage( pages.get( 1 ), "3", "4" );
    validator.validatePage( pages.get( 2 ), "5", "6" );
    validator.validatePage( pages.get( 3 ), "7", "8" );
    validator.validatePage( pages.get( 4 ), "9", "10" );
  }

  /*
   * A test with a large page header (height = 200). It does not suit 98 gap.
   *
   * Expected layout:
   *  01: ph,th,1
   *  ............
   *  10: ph,th,10
   */
  @Test
  public void paginate_WithHeaderAndLargePageHeader() throws Exception {
    String file = "table-layout-pagination-page-header-large.prpt";
    List<LogicalPageBox> pages = loadPages( file, 10 );

    PageValidator validator = validator().withPageHeader( 200 ).withTableHeader().build();
    for ( int i = 0; i < 10; i++ ) {
      validator.validatePage( pages.get( i ), Integer.toString( i + 1 ) );
    }
  }

  @Test
  public void paginate_WithHeaderAndSmallPageFooter() throws Exception {
    String file = "table-layout-pagination-page-footer-small.prpt";
    List<LogicalPageBox> pages = loadPages( file, 5 );

    PageValidator validator = validator().withTableHeader().build().checkPageFooter( 50 );
    validator.validatePage( pages.get( 0 ), "1", "2" );
    validator.validatePage( pages.get( 1 ), "3", "4" );
    validator.validatePage( pages.get( 2 ), "5", "6" );
    validator.validatePage( pages.get( 3 ), "7", "8" );
    validator.validatePage( pages.get( 4 ), "9", "10" );
  }

  @Test
  public void paginate_WithHeaderAndLargePageFooter() throws Exception {
    String file = "table-layout-pagination-page-footer-large.prpt";
    List<LogicalPageBox> pages = loadPages( file, 10 );

    PageValidator validator = validator().withTableHeader().build().checkPageFooter( 200 );
    for ( int i = 0; i < 10; i++ ) {
      validator.validatePage( pages.get( i ), Integer.toString( i + 1 ) );
    }
  }

  @Test
  public void paginate_WithHeaderAndSmallPageHeaderAndFooter() throws Exception {
    String file = "table-layout-pagination-page-header-footer-small.prpt";
    List<LogicalPageBox> pages = loadPages( file, 10 );

    PageValidator validator = validator().withPageHeader( 50 ).withTableHeader().build().checkPageFooter( 50 );
    for ( int i = 0; i < 10; i++ ) {
      validator.validatePage( pages.get( i ), Integer.toString( i + 1 ) );
    }
  }

  @Test
  public void paginate_WithHeaderAndFooterAndSmallPageHeaderAndFooter() throws Exception {
    String file = "table-layout-pagination-page-header-footer-small-table-header-footer.prpt";
    List<LogicalPageBox> pages = loadPages( file, 10 );

    PageValidator validator = validator().withPageHeader( 50 ).withTableHeader().build().checkPageFooter( 50 );
    for ( int i = 0; i < 9; i++ ) {
      validator.validatePage( pages.get( i ), Integer.toString( i + 1 ) );
    }
    validator.checkTableFooter().validatePage( pages.get( 9 ), Integer.toString( 10 ) );
  }



  @Test
  public void nested() throws Exception {
    String file = "table-layout-pagination-nested-table-header-footer.prpt";
    List<LogicalPageBox> pages = loadPages( file, 20 );
    System.out.println();
  }



  private static ValidatorBuilder validator() {
    return new ValidatorBuilder();
  }

  private static class ValidatorBuilder {
    private int pageHeader;
    private int tableHeader;

    public ValidatorBuilder withPageHeader( int height ) {
      pageHeader = height;
      return this;
    }

    public ValidatorBuilder withTableHeader() {
      tableHeader = HEADER_HEIGHT;
      return this;
    }

    public PageValidator build() {
      return new PageValidator( pageHeader, tableHeader );
    }
  }

  private static class PageValidator {
    private final int pageHeaderHeight;
    private final int tableHeaderHeight;
    private final int tableFooterHeight;
    private final int pageFooterHeight;

    public PageValidator( int pageHeaderHeight, int tableHeaderHeight ) {
      this( pageHeaderHeight, tableHeaderHeight, 0, 0 );
    }

    public PageValidator( int pageHeaderHeight, int tableHeaderHeight, int tableFooterHeight, int pageFooterHeight ) {
      this.pageHeaderHeight = pageHeaderHeight;
      this.tableHeaderHeight = tableHeaderHeight;
      this.tableFooterHeight = tableFooterHeight;
      this.pageFooterHeight = pageFooterHeight;
    }

    public PageValidator checkTableFooter() {
      return new PageValidator( pageHeaderHeight, tableHeaderHeight, FOOTER_HEIGHT, pageFooterHeight );
    }

    public PageValidator checkPageFooter( int footerHeight ) {
      return new PageValidator( pageHeaderHeight, tableHeaderHeight, tableFooterHeight, footerHeight );
    }

    public void validatePage( LogicalPageBox page, String... rows ) {
      int shift = 0;
      if ( pageHeaderHeight > 0 ) {
        assertPageHeader( page, shift );
        shift += pageHeaderHeight;
      }

      if ( tableHeaderHeight > 0 ) {
        assertTableHeader( page, shift );
        shift += tableHeaderHeight;
      }

      if ( !ArrayUtils.isEmpty( rows ) ) {
        assertRowsOnPage( page, rows, shift );
        shift += rows.length * ROW_HEIGHT;
      }

      if ( tableFooterHeight > 0 ) {
        assertTableFooter( page, shift );
      }

      if ( pageFooterHeight > 0 ) {
        assertPageFooter( page );
      }
    }

    private void assertPageHeader( LogicalPageBox page, int shift ) {
      RenderNode[] nodes = MatchFactory.findElementsByName( page, PAGE_HEADER_NAME );
      assertEquals( "Name lookup returned the paragraph and renderable text", 2, nodes.length );

      assertEquals( StrictGeomUtility.toInternalValue( shift ), nodes[ 0 ].getY() );
      assertEquals( PAGE_HEADER_VALUE, ( (RenderableText) nodes[ 1 ] ).getRawText() );
    }

    private void assertTableHeader( LogicalPageBox page, int shift ) {
      RenderNode[] nodes = MatchFactory.findElementsByName( page, HEADER_NAME );
      assertEquals( "Name lookup returned the paragraph and renderable text", 2, nodes.length );

      assertEquals( StrictGeomUtility.toInternalValue( shift ), nodes[ 0 ].getY() );
      assertEquals( HEADER_VALUE, ( (RenderableText) nodes[ 1 ] ).getRawText() );
    }

    private void assertRowsOnPage( LogicalPageBox page, String[] rows, long shift ) {
      // returns paragraphs and text nodes
      RenderNode[] rowsNodes = MatchFactory.findElementsByName( page, DETAIL_ROW_NAME );
      assertEquals( String.format( "Expected for find all these rows: %s\n, but actually found: %s",
          Arrays.toString( rows ), Arrays.toString( rowsNodes ) ),
        rows.length * 2, rowsNodes.length );

      for ( int i = 0; i < rows.length; i++ ) {
        String expectedRow = rows[ i ];
        RenderableText found = (RenderableText) rowsNodes[ i * 2 + 1 ];
        assertEquals( expectedRow, found.getRawText() );

        RenderNode paragraphNode = rowsNodes[ i * 2 ];
        long expectedY = StrictGeomUtility.toInternalValue( shift + i * ROW_HEIGHT );
        assertEquals( expectedY, paragraphNode.getY() );
      }
    }

    private void assertTableFooter( LogicalPageBox page, int shift ) {
      RenderNode[] nodes = MatchFactory.findElementsByName( page, FOOTER_NAME );
      assertEquals( "Name lookup returned the paragraph and renderable text", 2, nodes.length );

      assertEquals( FOOTER_VALUE, ( (RenderableText) nodes[ 1 ] ).getRawText() );
      assertEquals( StrictGeomUtility.toInternalValue( shift ), nodes[ 0 ].getY() );
    }

    private void assertPageFooter( LogicalPageBox page ) {
      RenderNode[] nodes = MatchFactory.findElementsByName( page, PAGE_FOOTER_NAME );
      assertEquals( "Name lookup returned the paragraph and renderable text", 2, nodes.length );


      assertEquals( PAGE_FOOTER_VALUE, ( (RenderableText) nodes[ 1 ] ).getRawText() );
      assertEquals( StrictGeomUtility.toInternalValue( pageFooterHeight ), nodes[ 0 ].getHeight() );
      assertEquals( page.getPageEnd(), nodes[ 0 ].getY2() );
    }
  }
}
