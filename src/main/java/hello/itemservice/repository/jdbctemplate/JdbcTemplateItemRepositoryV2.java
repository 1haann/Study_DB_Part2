package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NamedParameterJdbcTemplate
 * NOTE : 이름 지정 바인딩에서 자주 사용하는 3가지의 파라미터 종류
 *  - Map
 *  - SqlParameterSource
 *      -- BeanPropertySqlParameterSource
 *          자동으로 파라미터 객체를 생성한다.
 *          ex) getItemName()이 있다면 자동으로 key = itemName, value = 상품명 데이터를 만들어낸다.
 *          update() 메소드처럼 클래스 외부에 존재하는 "id"값을 포함하여 바인딩 하려면 Map 또는 MapSqlParameterSource를 사용해야 한다.
 *      -- MapSqlParameterSource
 *          Map과 유사하며 SQL 타입을 지정할 수 있는 등 SQL에 특화된 기능을 제공한다.
 *  파라미터를 전달하려면 Map처럼 key, value 데이터 구조를 만들어서 전달한다.
 *  key는 ":파라미터이름"으로 지정한 파라미터 이름이며 value는 해당 파라미터의 값이 된다.
 * BeanPropertyRowMapper
 */
@Slf4j
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;

    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) " +
                "values (:itemName, :price, :quantity)";

        // NOTE : 객체 item을 가지고 파라미터를 만든다.
        //  V1의 경우 파라미터의 순서대로 바인딩이 되며 순서가 틀린 경우 price에 quantity, quantity에 price가
        //  들어가는 문제가 발생할 수 있다. V2는 순서가 아니라 이름으로 파라미터 바인딩을 한다.
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(sql, param, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item " +
                "set item_name=:itemName, price=:price, quantity=:quantity " +
                "where id=:id";
        // NOTE param에 "itemName"라는 Key와 "updateParam.getItemName()"이라는 Value가 있다.
        //  sql문의 ":itemName"의 값으로 param의 "itemName"이라는 Key를 가진 "updateParam.getItemName()" 값이 들어간다.
        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);

        template.update(sql, param);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";
        try {
            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            // NOTE : queryForObject()는 데이터가 없는 경우 EmptyResultDataAccessException 예외가 터진다.
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";
        //동적 쿼리
        // NOTE : JdbcTemplate의 단점
        //  아래의 코드처럼 조건에 따라 where 또는 and를 넣는 등 경우의 수를 모두 계산하여 작성해야 하므로 복잡하며
        //  각 상황에 맞게 파라미터도 생성해야 하는 단점이 있다. 이후에 배울 MyBatis의 장점은 이런 동적 쿼리를 쉽게 작성할 수 있다는 것이다.
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }
        boolean andFlag = false;

        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            andFlag = true;
        }
        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }
        log.info("sql={}", sql);
        // NOTE : queryForObject()는 1개를 가져오며 query()는 여려개를 가져올 떄 사용한다.
        return template.query(sql, param, itemRowMapper());
    }

    // NOTE : RowMapper는 데이터베이스의 반환 결과인 ResultSet을 객체로 변환합니다.
    private RowMapper<Item> itemRowMapper() {
        // NOTE : 자바 객체는 camelCase 표기법을 사용하며 관계형 데이터베이스는 snake_case 표기법을 사용한다.
        //  BeanPropertyRowMapper는 ResultSet의 결과를 받고 자바빈 규약에 맞춰 데이터를 변환한다.
        //  ex) select id로 로 조회 시, Item 인스턴스 생성 후 setId(rs.getLong("id"));와 같은 코드를 작성해준다.
        //  BeanPropertyRowMapper는 언더스코어 표기법을 camel로 자동 변환해준다.
        //  즉 select item_name로 조회 시, setitem_name()이 아닌 setItemName()으로 변환해서 작동한다.
        //  ex) DB의 컬럼명(member_name)과 객체의 변수명(username)처럼 이름이 완전히 다르다면
        //      sql문에 "as"로 별칭을 주어 해결한다. select member_name as username
        return BeanPropertyRowMapper.newInstance(Item.class);   //camel 변환 지원
    }

}
