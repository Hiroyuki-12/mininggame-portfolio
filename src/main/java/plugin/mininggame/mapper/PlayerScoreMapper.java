package plugin.mininggame.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import plugin.mininggame.mapper.data.PlayerScore;

import java.util.List;

public interface PlayerScoreMapper {

    @Select("select * from mininggame_score;")
    List<PlayerScore> selectList();

    @Insert("INSERT INTO mininggame_score (player_name, score, registered_at) values (#{playerName}, #{score}, now())")
    void insert(PlayerScore playerScore);
}

