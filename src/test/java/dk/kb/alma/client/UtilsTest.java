package dk.kb.alma.client;

import com.google.common.collect.ImmutableList;
import dk.kb.alma.client.utils.Utils;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UtilsTest {
    
    @Test
    public void testCollectionsEmptyListImmutable() {
        List<String> immutable = Collections.emptyList();
        try {
            immutable.add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        List<String> list = Utils.toModifiableList(immutable);
        Assertions.assertEquals(0,list.size());
        list.add("Test");
        Assertions.assertEquals(list.get(0),("Test"));
    }
    
    @Test
    public void testListsOfImmutable() {
        List<String> immutable = List.of();
        try {
            immutable.add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        List<String> list = Utils.toModifiableList(immutable);
        Assertions.assertEquals(0,list.size());
        list.add("Test");
        Assertions.assertEquals(list.get(0),("Test"));
    }
    
    @Test
    public void testArraysAsListImmutable() {
        List<String> immutable = Arrays.asList();
        try {
            immutable.add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        List<String> list = Utils.toModifiableList(immutable);
        Assertions.assertEquals(0,list.size());
        list.add("Test");
        Assertions.assertEquals(list.get(0),("Test"));
    }
    
    @Test
    public void testGuavaImmutable() {
        List<String> immutable = ImmutableList.of();
        try {
            immutable.add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        List<String> list = Utils.toModifiableList(immutable);
        Assertions.assertEquals(0,list.size());
        Assertions.assertEquals(0,list.size());
        list.add("Test");
        Assertions.assertEquals(list.get(0),("Test"));
    }
    
    @Test
    public void testApacheCollections() {
        List<String> immutable = new UnmodifiableList<>(new ArrayList<>());
        try {
            immutable.add("test");
            Assertions.fail("empty list is not immutable, what magic is this?");
        } catch (UnsupportedOperationException e){
            //expected
        }
        List<String> list = Utils.toModifiableList(immutable);
        Assertions.assertEquals(0,list.size());
        list.add("Test");
        Assertions.assertEquals(list.get(0),("Test"));
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
