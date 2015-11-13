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

import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.filter.types.AutoLayoutBoxType;
import org.pentaho.reporting.engine.classic.core.layout.model.AutoRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.BreakMarkerRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.PageBreakPositionList;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.table.TableSectionRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.process.util.BlockLevelPaginationShiftState;
import org.pentaho.reporting.engine.classic.core.layout.process.util.InitialPaginationShiftState;
import org.pentaho.reporting.engine.classic.core.layout.process.util.PaginationTableState;
import org.pentaho.reporting.engine.classic.core.util.InstanceID;

import static org.junit.Assert.*;
import static org.pentaho.reporting.engine.classic.core.ReportAttributeMap.EMPTY_MAP;
import static org.pentaho.reporting.engine.classic.core.layout.model.context.BoxDefinition.EMPTY;
import static org.pentaho.reporting.engine.classic.core.layout.style.SimpleStyleSheet.EMPTY_STYLE;

/**
 * @author Andrey Khayrutdinov
 */
@SuppressWarnings( "deprecation" )
public class PaginationStepTest {

  @BeforeClass
  public static void init() throws Exception {
    ClassicEngineBoot.getInstance().start();
  }


  @Test
  public void updatesAutoBoxCoordinate_OnCompletionProcessingTableLevel() throws Exception {
    TableSectionRenderBox section = new TableSectionRenderBox();
    section.setY( 50 );
    section.setHeight( 20 );

    AutoRenderBox box = new AutoRenderBox();
    box.addChild( section );

    testUpdatesAutoBoxCoordinateOnCompletionProcessingTableLevel( box, 50 );
  }

  @Test
  public void updatesAutoBoxCoordinate_OnCompletionProcessingTableLevel_NestedContainer() throws Exception {
    TableSectionRenderBox section = new TableSectionRenderBox();
    section.setY( 50 );
    section.setHeight( 20 );

    AutoRenderBox nestedBox = new AutoRenderBox();
    nestedBox.addChild( section );

    AutoRenderBox box = new AutoRenderBox();
    box.addChild( nestedBox );

    testUpdatesAutoBoxCoordinateOnCompletionProcessingTableLevel( box, 50 );
  }

  @Test
  public void updatesAutoBoxCoordinate_OnCompletionProcessingTableLevel_NestedContainerWithPageBreaks()
    throws Exception {
    TableSectionRenderBox section = new TableSectionRenderBox();
    section.setY( 50 );
    section.setHeight( 20 );

    AutoRenderBox nestedBox = new AutoRenderBox();
    nestedBox.addChild( section );

    AutoRenderBox box = new AutoRenderBox();
    box.addChild( createTestBreakMarker() );
    box.addChild( nestedBox );

    testUpdatesAutoBoxCoordinateOnCompletionProcessingTableLevel( box, 50 );
  }

  private void testUpdatesAutoBoxCoordinateOnCompletionProcessingTableLevel( AutoRenderBox box, long expectedY )
    throws Exception {
    assertNotSame( expectedY, 0 );
    box.setY( 0 );

    PaginationStep step = createTestStepFor( box );
    step.finishTableLevelBox( box );
    assertEquals( "AutoRenderBox's coordinate should be updated as well", expectedY, box.getY() );
  }

  private static BreakMarkerRenderBox createTestBreakMarker() {
    return new BreakMarkerRenderBox(
      EMPTY_STYLE, new InstanceID(), EMPTY, AutoLayoutBoxType.INSTANCE, EMPTY_MAP, null, 0 );
  }

  private static PaginationStep createTestStepFor( RenderBox box ) {
    PaginationStep step = new PaginationStep();
    step.setPaginationTableState( new PaginationTableState( 100, 0, 100, new PageBreakPositionList() ) );

    BlockLevelPaginationShiftState shiftState = new BlockLevelPaginationShiftState();
    shiftState.reuse( null, new InitialPaginationShiftState(), box );
    step.setShiftState( shiftState );
    return step;
  }


  @Test
  public void gathersGapsBetweenPagebreaksAndPageEdges() throws Exception {
    BreakMarkerRenderBox marker = createTestBreakMarker();
    marker.setY( 70 );

    PaginationStep step = createTestStepFor( marker );
    PageBreakPositionList breaks = step.getBasePageBreakList();
    breaks.addMajorBreak( step.getPaginationTableState().getPageEnd(), 0 );

    step.finishTableLevelBox( marker );

    long expectedGap = 100 - 70;
    assertEquals( expectedGap, step.getActualShift() );
    assertEquals( expectedGap, step.getShiftState().getShiftForNextChild() );
  }
}
