package dk.kb.alma.client;

import dk.kb.alma.client.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UtilsTest {
    
    @Test
    public void testCollectionsEmptyListImmutable() {
        try {
            Collections.emptyList().add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        Utils.toModifiableList(Collections.emptyList()).add("Test");
    }
    
    @Test
    public void testListsOfImmutable() {
        try {
            List.of().add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        Utils.toModifiableList(List.of()).add("Test");
    }
    
    @Test
    public void testArraysAsListImmutable() {
        try {
            Arrays.asList().add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        Utils.toModifiableList(Arrays.asList()).add("Test");
    }
    
    @Test
    public void testSameList() {
        List<String> testList = new LinkedList<>();
        List<String> modifiableTestList = Utils.toModifiableList(testList);
        //Same object, not same contents in new wrapping
        Assertions.assertEquals(testList, modifiableTestList);
        Assertions.assertEquals(0,testList.size());
    }
}
