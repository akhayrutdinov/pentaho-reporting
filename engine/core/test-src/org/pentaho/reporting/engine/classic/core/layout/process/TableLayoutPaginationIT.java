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
 *   <li>Header's and footer's height are 150</li>
 * </ul>
 *
 * By default, each test's dataset contains 10 elements, with names defining their order number (1..10).
 *
 * @author Andrey Khayrutdinov
 */
public class TableLayoutPaginationIT {

  private static final String HEADER_NAME = "header";
  private static final int HEADER_HEIGHT = 150;
  private static final String HEADER_VALUE = "Header";

  private static final String FOOTER_NAME = "footer";
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
    assertPage( pages.get( 0 ), "1", "2", "3" );
    assertPage( pages.get( 1 ), "4", "5", "6" );
    assertPage( pages.get( 2 ), "7", "8", "9" );
    assertPage( pages.get( 3 ), "10" );
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
    assertPage( pages.get( 0 ), true, "1", "2" );
    assertPage( pages.get( 1 ), true, "3", "4" );
    assertPage( pages.get( 2 ), true, "5", "6" );
    assertPage( pages.get( 3 ), true, "7", "8" );
    assertPage( pages.get( 4 ), true, "9", "10" );
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
    assertPage( pages.get( 0 ), false, "1", "2", "3" );
    assertPage( pages.get( 1 ), false, "4", "5", "6" );
    assertPage( pages.get( 2 ), false, "7", "8", "9" );
    assertPage( pages.get( 3 ), false, true, "10" );
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
    assertPage( pages.get( 0 ), true, "1", "2" );
    assertPage( pages.get( 1 ), true, "3", "4" );
    assertPage( pages.get( 2 ), true, "5", "6" );
    assertPage( pages.get( 3 ), true, "7", "8" );
    assertPage( pages.get( 4 ), true, "9", "10" );
    assertPage( pages.get( 5 ), true, true );
  }

  /*
   * A special test with 100 records. It is needed to reveal a possible mistakes
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
    int recordCounter = 1;
    for ( int i = 0; i < 50; i++ ) {
      String firstRow = Integer.toString( recordCounter++ );
      String secondRow = Integer.toString( recordCounter++ );
      assertPage( pages.get( i ), true, firstRow, secondRow );
    }
    assertPage( pages.get( 50 ), true, true );
  }


  private static void assertPage( LogicalPageBox page, String... rows ) {
    assertPage( page, false, rows );
  }

  private static void assertPage( LogicalPageBox page, boolean expectHeader, String... rows ) {
    assertPage( page, expectHeader, false, rows );
  }

  private static void assertPage( LogicalPageBox page, boolean expectHeader, boolean expectFooter, String... rows ) {
    if ( expectHeader ) {
      assertHeaderOnPage( page );
    }

    if ( !ArrayUtils.isEmpty( rows ) ) {
      assertDataRowsOnPage( page, expectHeader, rows );
    }

    if ( expectFooter ) {
      assertFooterOnPage( page, expectHeader, rows );
    }
  }

  private static void assertHeaderOnPage( LogicalPageBox page ) {
    RenderNode[] nodes = MatchFactory.findElementsByName( page, HEADER_NAME );
    assertEquals( "Name lookup returned the paragraph and renderable text", 2, nodes.length );

    assertEquals( "Header should be put in the very beginning", 0, nodes[ 0 ].getY() );
    assertEquals( HEADER_VALUE, ( (RenderableText) nodes[ 1 ] ).getRawText() );
  }

  private static void assertDataRowsOnPage( LogicalPageBox page, boolean expectHeader, String... rows ) {
    // returns paragraphs and text nodes
    RenderNode[] rowsNodes = MatchFactory.findElementsByName( page, DETAIL_ROW_NAME );
    assertEquals( String.format( "Expected for find all these rows: %s\n, but actually found: %s",
        Arrays.toString( rows ), Arrays.toString( rowsNodes ) ),
      rows.length * 2, rowsNodes.length );

    final int start = expectHeader ? HEADER_HEIGHT : 0;
    for ( int i = 0; i < rows.length; i++ ) {
      String expectedRow = rows[ i ];
      RenderableText found = (RenderableText) rowsNodes[ i * 2 + 1 ];
      assertEquals( expectedRow, found.getRawText() );

      RenderNode paragraphNode = rowsNodes[ i * 2 ];
      long expectedY = StrictGeomUtility.toInternalValue( start + i * ROW_HEIGHT );
      assertEquals( expectedY, paragraphNode.getY() );
    }
  }

  private static void assertFooterOnPage( LogicalPageBox page, boolean expectHeader, String... rows ) {
    RenderNode[] nodes = MatchFactory.findElementsByName( page, FOOTER_NAME );
    assertEquals( "Name lookup returned the paragraph and renderable text", 2, nodes.length );

    assertEquals( FOOTER_VALUE, ( (RenderableText) nodes[ 1 ] ).getRawText() );

    long footerY = expectHeader ? HEADER_HEIGHT : 0;
    if ( !ArrayUtils.isEmpty( rows ) ) {
      footerY += rows.length * ROW_HEIGHT;
    }
    footerY = StrictGeomUtility.toInternalValue( footerY );
    assertEquals( footerY, nodes[ 0 ].getY() );
  }
}
