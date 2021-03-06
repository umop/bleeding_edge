/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.ast.ASTFactory.compilationUnit;
import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.HashMap;

public class DartEntryImplTest extends EngineTestCase {
  public void test_creation() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    assertSame(CacheState.INVALID, entry.getState(DartEntry.ELEMENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.EXPORTED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IMPORTED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.INVALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PUBLIC_NAMESPACE));
    assertTrue(entry.hasInvalidData(DartEntry.SOURCE_KIND));
  }

  public void test_getAllErrors() {
    Source source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    assertLength(0, entry.getAllErrors());
    entry.setValue(DartEntry.PARSE_ERRORS, new AnalysisError[] {new AnalysisError(
        source,
        ParserErrorCode.ABSTRACT_CLASS_MEMBER)});
    entry.setValue(DartEntry.RESOLUTION_ERRORS, source, new AnalysisError[] {new AnalysisError(
        source,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_THROWS_EXCEPTION)});
    entry.setValue(DartEntry.VERIFICATION_ERRORS, source, new AnalysisError[] {new AnalysisError(
        source,
        StaticWarningCode.CASE_BLOCK_NOT_TERMINATED)});
    entry.setValue(DartEntry.HINTS, source, new AnalysisError[] {new AnalysisError(
        source,
        HintCode.DEAD_CODE)});
    assertLength(4, entry.getAllErrors());
  }

  public void test_getState_invalid_element() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.getState(DartEntry.ELEMENT, new TestSource());
      fail("Expected IllegalArgumentException for ELEMENT");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getState_invalid_resolutionErrors() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.getState(DartEntry.RESOLUTION_ERRORS);
      fail("Expected IllegalArgumentException for RESOLUTION_ERRORS");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getState_invalid_verificationErrors() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.getState(DartEntry.VERIFICATION_ERRORS);
      fail("Expected IllegalArgumentException for VERIFICATION_ERRORS");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getValue_invalid_element() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.getValue(DartEntry.ELEMENT, new TestSource());
      fail("Expected IllegalArgumentException for ELEMENT");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getValue_invalid_resolutionErrors() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.getValue(DartEntry.RESOLUTION_ERRORS);
      fail("Expected IllegalArgumentException for RESOLUTION_ERRORS");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getValue_invalid_resolutionErrors_multiple() {
    Source source1 = new TestSource();
    Source source2 = new TestSource();
    Source source3 = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    entry.setValue(DartEntry.RESOLVED_UNIT, source1, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source2, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source3, compilationUnit());
    try {
      entry.getValue(DartEntry.ELEMENT, source3);
      fail("Expected IllegalArgumentException for ELEMENT");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getValue_invalid_verificationErrors() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.getValue(DartEntry.VERIFICATION_ERRORS);
      fail("Expected IllegalArgumentException for VERIFICATION_ERRORS");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_getWritableCopy() {
    DartEntryImpl entry = new DartEntryImpl();
    DartEntryImpl copy = entry.getWritableCopy();
    assertNotNull(copy);
    assertNotSame(entry, copy);
  }

  public void test_hasInvalidData_false() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    entry.recordParseError();
    assertFalse(entry.hasInvalidData(DartEntry.ELEMENT));
    assertFalse(entry.hasInvalidData(DartEntry.EXPORTED_LIBRARIES));
    assertFalse(entry.hasInvalidData(DartEntry.IMPORTED_LIBRARIES));
    assertFalse(entry.hasInvalidData(DartEntry.INCLUDED_PARTS));
    assertFalse(entry.hasInvalidData(DartEntry.IS_CLIENT));
    assertFalse(entry.hasInvalidData(DartEntry.IS_LAUNCHABLE));
    assertFalse(entry.hasInvalidData(SourceEntry.LINE_INFO));
    assertFalse(entry.hasInvalidData(DartEntry.PARSE_ERRORS));
    assertFalse(entry.hasInvalidData(DartEntry.PARSED_UNIT));
    assertFalse(entry.hasInvalidData(DartEntry.PUBLIC_NAMESPACE));
    assertFalse(entry.hasInvalidData(DartEntry.SOURCE_KIND));
    assertFalse(entry.hasInvalidData(DartEntry.RESOLUTION_ERRORS));
    assertFalse(entry.hasInvalidData(DartEntry.RESOLVED_UNIT));
    assertFalse(entry.hasInvalidData(DartEntry.VERIFICATION_ERRORS));
    assertFalse(entry.hasInvalidData(DartEntry.HINTS));
  }

  public void test_hasInvalidData_true() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    assertTrue(entry.hasInvalidData(DartEntry.ELEMENT));
    assertTrue(entry.hasInvalidData(DartEntry.EXPORTED_LIBRARIES));
    assertTrue(entry.hasInvalidData(DartEntry.IMPORTED_LIBRARIES));
    assertTrue(entry.hasInvalidData(DartEntry.INCLUDED_PARTS));
    assertTrue(entry.hasInvalidData(DartEntry.IS_CLIENT));
    assertTrue(entry.hasInvalidData(DartEntry.IS_LAUNCHABLE));
    assertTrue(entry.hasInvalidData(SourceEntry.LINE_INFO));
    assertTrue(entry.hasInvalidData(DartEntry.PARSE_ERRORS));
    assertTrue(entry.hasInvalidData(DartEntry.PARSED_UNIT));
    assertTrue(entry.hasInvalidData(DartEntry.PUBLIC_NAMESPACE));
    assertTrue(entry.hasInvalidData(DartEntry.SOURCE_KIND));
    assertTrue(entry.hasInvalidData(DartEntry.RESOLUTION_ERRORS));
    assertTrue(entry.hasInvalidData(DartEntry.RESOLVED_UNIT));
    assertTrue(entry.hasInvalidData(DartEntry.VERIFICATION_ERRORS));
    assertTrue(entry.hasInvalidData(DartEntry.HINTS));
  }

  public void test_invalidateAllInformation() throws Exception {
    DartEntryImpl entry = entryWithValidState();
    entry.invalidateAllInformation();
    assertSame(CacheState.INVALID, entry.getState(DartEntry.ELEMENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.EXPORTED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IMPORTED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.INVALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PUBLIC_NAMESPACE));
  }

  public void test_invalidateAllResolutionInformation() throws Exception {
    DartEntryImpl entry = entryWithValidState();
    entry.invalidateAllResolutionInformation();
    assertSame(CacheState.INVALID, entry.getState(DartEntry.ELEMENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PUBLIC_NAMESPACE));
  }

  public void test_isClient() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    // true
    entry.setValue(DartEntry.IS_CLIENT, true);
    assertTrue(entry.getValue(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IS_CLIENT));
    // invalidate
    entry.setState(DartEntry.IS_CLIENT, CacheState.INVALID);
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_CLIENT));
    // false
    entry.setValue(DartEntry.IS_CLIENT, false);
    assertFalse(entry.getValue(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IS_CLIENT));
  }

  public void test_isLaunchable() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    // true
    entry.setValue(DartEntry.IS_LAUNCHABLE, true);
    assertTrue(entry.getValue(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IS_LAUNCHABLE));
    // invalidate
    entry.setState(DartEntry.IS_LAUNCHABLE, CacheState.INVALID);
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IS_LAUNCHABLE));
    // false
    entry.setValue(DartEntry.IS_LAUNCHABLE, false);
    assertFalse(entry.getValue(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IS_LAUNCHABLE));
  }

  public void test_recordParseError() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    entry.recordParseError();
    assertSame(CacheState.ERROR, entry.getState(DartEntry.ELEMENT));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.EXPORTED_LIBRARIES));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.IMPORTED_LIBRARIES));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.ERROR, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.PUBLIC_NAMESPACE));
  }

  public void test_recordParseInProcess() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    entry.recordParseInProcess();
    assertSame(CacheState.IN_PROCESS, entry.getState(DartEntry.EXPORTED_LIBRARIES));
    assertSame(CacheState.IN_PROCESS, entry.getState(DartEntry.IMPORTED_LIBRARIES));
    assertSame(CacheState.IN_PROCESS, entry.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.IN_PROCESS, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.IN_PROCESS, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.IN_PROCESS, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.IN_PROCESS, entry.getState(DartEntry.SOURCE_KIND));
  }

  public void test_recordResolutionError() throws Exception {
    DartEntryImpl entry = new DartEntryImpl();
    entry.recordResolutionError();
    assertSame(CacheState.ERROR, entry.getState(DartEntry.ELEMENT));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.EXPORTED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.IMPORTED_LIBRARIES));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.INVALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.ERROR, entry.getState(DartEntry.PUBLIC_NAMESPACE));
  }

  public void test_removeResolution_multiple_first() {
    Source source1 = new TestSource();
    Source source2 = new TestSource();
    Source source3 = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    entry.setValue(DartEntry.RESOLVED_UNIT, source1, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source2, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source3, compilationUnit());
    entry.removeResolution(source1);
  }

  public void test_removeResolution_multiple_last() {
    Source source1 = new TestSource();
    Source source2 = new TestSource();
    Source source3 = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    entry.setValue(DartEntry.RESOLVED_UNIT, source1, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source2, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source3, compilationUnit());
    entry.removeResolution(source3);
  }

  public void test_removeResolution_multiple_middle() {
    Source source1 = new TestSource();
    Source source2 = new TestSource();
    Source source3 = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    entry.setValue(DartEntry.RESOLVED_UNIT, source1, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source2, compilationUnit());
    entry.setValue(DartEntry.RESOLVED_UNIT, source3, compilationUnit());
    entry.removeResolution(source2);
  }

  public void test_removeResolution_single() {
    Source source1 = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    entry.setValue(DartEntry.RESOLVED_UNIT, source1, compilationUnit());
    entry.removeResolution(source1);
  }

  public void test_resolutionState() throws Exception {
    DartEntryImpl entry1 = new DartEntryImpl();

    Source libSrc1 = new TestSource(null, createFile("/test1.dart"), "");
    Source libSrc2 = new TestSource(null, createFile("/test2.dart"), "");

    ParserErrorCode errCode = ParserErrorCode.DIRECTIVE_AFTER_DECLARATION;
    AnalysisError[] errors1 = new AnalysisError[] {new AnalysisError(libSrc1, 0, 10, errCode)};
    AnalysisError[] errors2 = new AnalysisError[] {new AnalysisError(libSrc2, 0, 20, errCode)};

    entry1.setValue(DartEntry.RESOLUTION_ERRORS, libSrc1, errors1);
    entry1.setValue(DartEntry.RESOLUTION_ERRORS, libSrc2, errors2);

    TestDartEntryImpl entry2 = new TestDartEntryImpl();
    entry2.copyFrom(entry1);
    assertExactElements(entry2.getAllErrors(), errors1[0], errors2[0]);

    entry1.removeResolution(libSrc2);
    assertExactElements(entry1.getAllErrors(), errors1[0]);

    entry2.removeResolution(libSrc1);
    assertExactElements(entry2.getAllErrors(), errors2[0]);

    entry2.removeResolution(libSrc2);
    assertExactElements(entry2.getAllErrors());
  }

  public void test_setParseResults() {
    DartEntryImpl entry = new DartEntryImpl();
    assertSame(CacheState.INVALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.INVALID, entry.getState(DartEntry.PARSED_UNIT));
    entry.setParseResults(0L, null, null, null);
    assertSame(CacheState.VALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PARSED_UNIT));
  }

  public void test_setState_element() {
    setState2(DartEntry.ELEMENT);
  }

  public void test_setState_exportedLibraries() {
    setState2(DartEntry.EXPORTED_LIBRARIES);
  }

  public void test_setState_hints() {
    setState3(DartEntry.HINTS);
  }

  public void test_setState_importedLibraries() {
    setState2(DartEntry.IMPORTED_LIBRARIES);
  }

  public void test_setState_includedParts() {
    setState2(DartEntry.INCLUDED_PARTS);
  }

  public void test_setState_invalid_element() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.setState(DartEntry.ELEMENT, null, CacheState.FLUSHED);
      fail("Expected IllegalArgumentException for ELEMENT");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_setState_invalid_resolutionErrors() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.setState(DartEntry.RESOLUTION_ERRORS, CacheState.FLUSHED);
      fail("Expected IllegalArgumentException for RESOLUTION_ERRORS");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_setState_invalid_validState() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.setState(DartEntry.LINE_INFO, CacheState.VALID);
      fail("Expected IllegalArgumentException for a state of VALID");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_setState_invalid_verificationErrors() {
    DartEntryImpl entry = new DartEntryImpl();
    try {
      entry.setState(DartEntry.VERIFICATION_ERRORS, CacheState.FLUSHED);
      fail("Expected IllegalArgumentException for VERIFICATION_ERRORS");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_setState_isClient() {
    setState2(DartEntry.IS_CLIENT);
  }

  public void test_setState_isLaunchable() {
    setState2(DartEntry.IS_LAUNCHABLE);
  }

  public void test_setState_lineInfo() {
    setState2(SourceEntry.LINE_INFO);
  }

  public void test_setState_parsedUnit() {
    setState2(DartEntry.PARSED_UNIT);
  }

  public void test_setState_parseErrors() {
    setState2(DartEntry.PARSE_ERRORS);
  }

  public void test_setState_publicNamespace() {
    setState2(DartEntry.PUBLIC_NAMESPACE);
  }

  public void test_setState_resolutionErrors() {
    setState3(DartEntry.RESOLUTION_ERRORS);
  }

  public void test_setState_resolvedUnit() {
    setState3(DartEntry.RESOLVED_UNIT);
  }

  public void test_setState_sourceKind() {
    setState2(DartEntry.SOURCE_KIND);
  }

  public void test_setState_verificationErrors() {
    setState3(DartEntry.VERIFICATION_ERRORS);
  }

  public void test_setValue_element() {
    setValue2(DartEntry.ELEMENT, new LibraryElementImpl(null, libraryIdentifier("lib")));
  }

  public void test_setValue_exportedLibraries() {
    setValue2(DartEntry.EXPORTED_LIBRARIES, new Source[] {new TestSource()});
  }

  public void test_setValue_hints() {
    setValue3(DartEntry.HINTS, new AnalysisError[] {new AnalysisError(null, HintCode.DEAD_CODE)});
  }

  public void test_setValue_importedLibraries() {
    setValue2(DartEntry.IMPORTED_LIBRARIES, new Source[] {new TestSource()});
  }

  public void test_setValue_includedParts() {
    setValue2(DartEntry.INCLUDED_PARTS, new Source[] {new TestSource()});
  }

  public void test_setValue_isClient() {
    setValue2(DartEntry.IS_CLIENT, Boolean.TRUE);
  }

  public void test_setValue_isLaunchable() {
    setValue2(DartEntry.IS_LAUNCHABLE, Boolean.TRUE);
  }

  public void test_setValue_lineInfo() {
    setValue2(SourceEntry.LINE_INFO, new LineInfo(new int[] {0}));
  }

  public void test_setValue_parsedUnit() {
    setValue2(DartEntry.PARSED_UNIT, compilationUnit());
  }

  public void test_setValue_parseErrors() {
    setValue2(DartEntry.PARSE_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        ParserErrorCode.ABSTRACT_CLASS_MEMBER)});
  }

  public void test_setValue_publicNamespace() {
    setValue2(DartEntry.PUBLIC_NAMESPACE, new Namespace(new HashMap<String, Element>()));
  }

  public void test_setValue_resolutionErrors() {
    setValue3(DartEntry.RESOLUTION_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_THROWS_EXCEPTION)});
  }

  public void test_setValue_resolvedUnit() {
    setValue3(DartEntry.RESOLVED_UNIT, compilationUnit());
  }

  public void test_setValue_sourceKind() {
    setValue2(DartEntry.SOURCE_KIND, SourceKind.LIBRARY);
  }

  public void test_setValue_verificationErrors() {
    setValue3(DartEntry.VERIFICATION_ERRORS, new AnalysisError[] {new AnalysisError(
        null,
        StaticWarningCode.CASE_BLOCK_NOT_TERMINATED)});
  }

  private DartEntryImpl entryWithValidState() {
    DartEntryImpl entry = new DartEntryImpl();
    entry.setValue(DartEntry.ELEMENT, null);
    entry.setValue(DartEntry.EXPORTED_LIBRARIES, null);
    entry.setValue(DartEntry.IMPORTED_LIBRARIES, null);
    entry.setValue(DartEntry.INCLUDED_PARTS, null);
    entry.setValue(DartEntry.IS_CLIENT, true);
    entry.setValue(DartEntry.IS_LAUNCHABLE, true);
    entry.setValue(SourceEntry.LINE_INFO, null);
    entry.setValue(DartEntry.PARSE_ERRORS, null);
    entry.setValue(DartEntry.PARSED_UNIT, null);
    entry.setValue(DartEntry.PUBLIC_NAMESPACE, null);

    assertSame(CacheState.VALID, entry.getState(DartEntry.ELEMENT));
    assertSame(CacheState.VALID, entry.getState(DartEntry.EXPORTED_LIBRARIES));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IMPORTED_LIBRARIES));
    assertSame(CacheState.VALID, entry.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, entry.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, entry.getState(SourceEntry.LINE_INFO));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.VALID, entry.getState(DartEntry.PUBLIC_NAMESPACE));
    return entry;
  }

  private void setState2(DataDescriptor<?> descriptor) {
    DartEntryImpl entry = new DartEntryImpl();
    assertNotSame(CacheState.FLUSHED, entry.getState(descriptor));
    entry.setState(descriptor, CacheState.FLUSHED);
    assertSame(CacheState.FLUSHED, entry.getState(descriptor));
  }

  private void setState3(DataDescriptor<?> descriptor) {
    Source source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    assertNotSame(CacheState.FLUSHED, entry.getState(descriptor, source));
    entry.setState(descriptor, source, CacheState.FLUSHED);
    assertSame(CacheState.FLUSHED, entry.getState(descriptor, source));
  }

  private <E> void setValue2(DataDescriptor<E> descriptor, E newValue) {
    DartEntryImpl entry = new DartEntryImpl();
    E value = entry.getValue(descriptor);
    assertNotSame(value, newValue);
    entry.setValue(descriptor, newValue);
    assertSame(CacheState.VALID, entry.getState(descriptor));
    assertSame(newValue, entry.getValue(descriptor));
  }

  private <E> void setValue3(DataDescriptor<E> descriptor, E newValue) {
    Source source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    E value = entry.getValue(descriptor, source);
    assertNotSame(value, newValue);
    entry.setValue(descriptor, source, newValue);
    assertSame(CacheState.VALID, entry.getState(descriptor, source));
    assertSame(newValue, entry.getValue(descriptor, source));
  }
}
