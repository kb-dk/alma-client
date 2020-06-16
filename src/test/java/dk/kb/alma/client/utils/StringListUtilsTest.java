package dk.kb.alma.client.utils;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

class StringListUtilsTest {
    
    @Test
    void cutMiddle() {
    
        String string
                = "SU1:SU1_DESK:HOLD,MOVE:HFBOT,HFGEO,HFPAN,HFZM,HFFAR,HFJTS,HFJSP,HFKVT,HAUDL,HATSS,HFFJ,HFTSS,LFZM,LFPAN,LFBOT,LFGEO,LFJTS,LFKVT,LFZM,LFFJ,HASEM,BAGEO,BAIVT,BALSA,BAPEN,BAHBS,BAINF,BAINH,BASEM";
    
        String result = StringListUtils.cutMiddle(string, 100);
        MatcherAssert.assertThat(result.length(),is(100));
        MatcherAssert.assertThat(result,is("SU1:SU1_DESK:HOLD,MOVE:HFBOT,HFGEO,HFPAN,HFZM,HF...M,BAGEO,BAIVT,BALSA,BAPEN,BAHBS,BAINF,BAINH,BASEM"));
    }
    
    @Test
    void cutEnd() {
        
        String string
                = "SU1:SU1_DESK:HOLD,MOVE:HFBOT,HFGEO,HFPAN,HFZM,HFFAR,HFJTS,HFJSP,HFKVT,HAUDL,HATSS,HFFJ,HFTSS,LFZM,LFPAN,LFBOT,LFGEO,LFJTS,LFKVT,LFZM,LFFJ,HASEM,BAGEO,BAIVT,BALSA,BAPEN,BAHBS,BAINF,BAINH,BASEM";
        
        String result = StringListUtils.cutEnd(string, 100);
        MatcherAssert.assertThat(result.length(),is(100));
        MatcherAssert.assertThat(result,is("SU1:SU1_DESK:HOLD,MOVE:HFBOT,HFGEO,HFPAN,HFZM,HFFAR,HFJTS,HFJSP,HFKVT,HAUDL,HATSS,HFFJ,HFTSS,LFZM..."));

    }
}