package hello.itemservice;

import hello.itemservice.config.*;
import hello.itemservice.repository.ItemRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;


//@Import(MemoryConfig.class)
// NOTE : JdbcTemplate 사용
//@Import(JdbcTemplateV1Config.class)
//@Import(JdbcTemplateV2Config.class)
@Import(JdbcTemplateV3Config.class)
@SpringBootApplication(scanBasePackages = "hello.itemservice.web")
public class ItemServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemServiceApplication.class, args);
	}

	// NOTE : 사용 방법
	//   @Profile은 spring.profiles.active 속성을 읽어서 프로필로 사용한다.
	//	 @Profile("local")의 경우, application.properties 내부에 spring.profiles.active의 값이 local인 경우만 Bean으로 등록

	// NOTE : @Profile 사용 이유
	//	 		- testDataInit 메소드를 통해 테스트용 더미 데이터를 가져와 local환경에서 실행을 하여 테스트를 할 수 있는 편리함도 있지만
	//			  테스트 코드를 작성하여 실행을 했을 때 testDataInit 메소드의 더미 데이터가 섞여 들어와 테스트 데이터가 오염됩니다.
	//			  ex) 테스트 코드에서 save()를 통해 데이터 하나를 저장하고 카운트를 했으나 tesDataInit의 2개의 데이터로 인해 3개가 카운트 되는 문제 발생
	//			- main 내에 위치한 spring.profiles.active=local로, test 내에 위치한 spring.profiles.active=test로 작성을 하여
	//			  테스트 코드에서는 testDataInit 메소드를 실행하지 않고 테스트용 더미 데이터 역시 생성되지 않기 때문에 정확한 테스트가 가능하다.

	@Bean
	@Profile("local")
	public TestDataInit testDataInit(ItemRepository itemRepository) {
		return new TestDataInit(itemRepository);
	}

}
