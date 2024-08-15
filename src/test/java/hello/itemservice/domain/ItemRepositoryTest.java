package hello.itemservice.domain;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import hello.itemservice.repository.memory.MemoryItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// NOTE: @SpringBootTest는 상위 패키지로 올라가며 @SpringBootApplication을 찾아서 설정으로 사용한다.
//  현재는 @Import(JdbcTemplateV3Config.class)로 JdbcTemplateV3 설정을 가지고 테스트를 진행한다.

// NOTE: @Transactional 어노테이션은 로직이 성공적으로 수행되면 커밋하도록 동작한다.
//  그러나 테스트에서 @Transactional을 사용하면 스프링은 테스트를 트랜잭션 안에서 실행하고 테스트가 끝나면 트랜잭션을 자동으로 롤백시킨다.
//  클래스 또는 메소드에 붙여 사용한다.

// NOTE: @Commit(강제로 커밋하기)
//  @Transactional을 테스트에서 사용하면 테스트가 끝난 후 롤백되기 때문에 테스트 과정에서 저장한 모든 데이터가 사라진다.
//  실제로 데이터베이스에 데이터가 잘 보관되었는지 확인해보고 싶다면 @Commit을 클래스 또는 메소드에 붙여 사용한다.
//  @Commit을 붙이면 테스트 종료 후 롤백 대신 커밋이 호출된다. @Rollback(value = false)처럼 작성을 하는 방법도 있다.

//@Commit
@Transactional
@SpringBootTest
class ItemRepositoryTest {

    /* MemoryItemRepository 구현체가 아닌 ItemRepository 인터페이스를 테스트 하는 이유
    *  인터페이스를 대상으로 테스트하면 향후 다른 구현체로 변경되었을 때 테스트 코드 수정 없이 해당 구현체가 잘 동작하는지 검증이 가능하다.
    *  */
    @Autowired
    ItemRepository itemRepository;

//    @Autowired
//    PlatformTransactionManager transactionManager;
//    TransactionStatus status;
//    @BeforeEach
//    void beforeEach() {
//        //트랜잭션 시작
//        status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//    }

    @AfterEach
    void afterEach() {
        //MemoryItemRepository 의 경우 제한적으로 사용
        if (itemRepository instanceof MemoryItemRepository) {
            ((MemoryItemRepository) itemRepository).clearStore();
        }
        //트랜잭션 롤백
        //transactionManager.rollback(status);
    }

    @Test
    void save() {
        //given
        Item item = new Item("itemA", 10000, 10);

        //when
        Item savedItem = itemRepository.save(item);

        //then
        Item findItem = itemRepository.findById(item.getId()).get();
        assertThat(findItem).isEqualTo(savedItem);
    }

    @Test
    void updateItem() {
        //given
        Item item = new Item("item1", 10000, 10);
        Item savedItem = itemRepository.save(item);
        Long itemId = savedItem.getId();

        //when
        ItemUpdateDto updateParam = new ItemUpdateDto("item2", 20000, 30);
        itemRepository.update(itemId, updateParam);

        //then
        Item findItem = itemRepository.findById(itemId).get();
        assertThat(findItem.getItemName()).isEqualTo(updateParam.getItemName());
        assertThat(findItem.getPrice()).isEqualTo(updateParam.getPrice());
        assertThat(findItem.getQuantity()).isEqualTo(updateParam.getQuantity());
    }

    // NOTE : 별도의 테스트용 데이터베이스를 생성하여 테스트를 진행해도 문제가 발생한다.
    //  ex) findItems() 메소드를 여러번 실행하면 데이터가 계속 쌓이며 테스트에 영향을 준다.
    //  테스트의 중요한 원칙
    //      - 테스트는 다른 테스트와 격리해야 한다.
    //      - 테스트는 반복해서 실행할 수 있어야 한다.
    //  위의 문제를 해결하려면 각각의 테스트가 끝날 때 마다 해당 테스트에서 추가한 데이터를 삭제해야 한다.
    //  각각의 테스트가 끝날 때 마다 데이터 롤백을 하여 추가했던 데이터를 삭제한다.
    //      - 1.트랜잭션 시작 2.테스트 A 실행 3.트랜잭션 롤백
    //      - 4.트랜잭션 시작 5.테스트 B 실행 6.트랜잭션 롤백
    @Test
    void findItems() {
        //given
        Item item1 = new Item("itemA-1", 10000, 10);
        Item item2 = new Item("itemA-2", 20000, 20);
        Item item3 = new Item("itemB-1", 30000, 30);

        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);

        //둘 다 없음 검증
        test(null, null, item1, item2, item3);
        test("", null, item1, item2, item3);

        //itemName 검증
        test("itemA", null, item1, item2);
        test("temA", null, item1, item2);
        test("itemB", null, item3);

        //maxPrice 검증
        test(null, 10000, item1);

        //둘 다 있음 검증
        test("itemA", 10000, item1);
    }

    void test(String itemName, Integer maxPrice, Item... items) {
        List<Item> result = itemRepository.findAll(new ItemSearchCond(itemName, maxPrice));
        assertThat(result).containsExactly(items);
    }
}
