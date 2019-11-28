package org.apache.isis.extensions.fakedata.dom;

import java.util.UUID;

public class Uuids extends AbstractRandomValueGenerator{

    public Uuids(final FakeDataService fakeDataService) {
        super(fakeDataService);
    }

    public UUID any() {
        return UUID.randomUUID();
    }
}