package org.janelia.saalfeldlab.n5.universe.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JqPredicate<S> implements Predicate<S>{

	private final Scope scope;

	private final ObjectMapper objMapper;

	private final Gson gson;

	private final JsonQuery query;

	public JqPredicate(final String translation, final Gson gson ) {
		this.gson = gson;

		scope = JqUtils.buildRootScope();
		objMapper = new ObjectMapper();

		JsonQuery qTmp = null;
		try {
			qTmp = JsonQuery.compile(JqUtils.resolveImports(translation), Versions.JQ_1_6);
		} catch (JsonQueryException e) {
			e.printStackTrace();
		}
		query = qTmp;
	}

	@Override
	public boolean test(S src) {
		if( query == null )
			return false;

		JsonNode jsonNode;
		try {
			jsonNode = objMapper.readTree(gson.toJson(src));

			final List<JsonNode> out = new ArrayList<>();
			query.apply(scope, jsonNode, out::add);

			final StringBuffer stringOutput = new StringBuffer();
			for (final JsonNode n : out)
				stringOutput.append(n.toString() + "\n");

			return gson.fromJson(stringOutput.toString(), Boolean.class);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

}
