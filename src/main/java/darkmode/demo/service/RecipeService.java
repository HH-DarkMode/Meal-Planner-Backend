package darkmode.demo.service;

import darkmode.demo.entity.Kriteerit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RecipeService {

	private final ChatClient chatClient;

	public RecipeService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	/* Reseptihaun hakukriteerit */
	public String suggestRecipe(Kriteerit kriteerit) {
		List<String> criteria = new ArrayList<>();
		
		if (kriteerit.getKalorit() != null) {
			criteria.add("- Kalorit: %d kcal".formatted(kriteerit.getKalorit()));
		}
		
		if (kriteerit.getHiilihydraatit() != null) {
			criteria.add("- Hiilihydraatit: %d g".formatted(kriteerit.getHiilihydraatit()));
		}
		
		if (kriteerit.getProteiini() != null) {
			criteria.add("- Proteiini: %d g".formatted(kriteerit.getProteiini()));
		}
		
		if (kriteerit.getRasva() != null) {
			criteria.add("- Rasva: %d g".formatted(kriteerit.getRasva()));
		}
		
		if (kriteerit.getAnnoskoko() != null) {
			criteria.add("- Annoskoko: %d g".formatted(kriteerit.getAnnoskoko()));
		}

		String criteriaText = criteria.isEmpty()
				? "Ei ravintoarvovaatimuksia, ehdota vapaasti jotain ruokaohjetta."
				: String.join("\n", criteria);

		String prompt = """
				Ehdota yksi ruokaohje, joka vastaa mahdollisimman hyvin seuraavia \
				ravintoarvoja annosta kohden:
				%s

				Vastaa suomeksi. Kerro reseptin nimi, raaka-aineet ja valmistusohjeet.
				"""
				.formatted(criteriaText);

		return chatClient.prompt()
				.user(prompt)
				.call()
				.content();
	}
}
