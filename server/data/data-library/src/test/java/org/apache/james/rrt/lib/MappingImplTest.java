package org.apache.james.rrt.lib;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MappingImplTest {

    @Test(expected=NullPointerException.class)
    public void addressFactoryMethodShouldThrowOnNull() {
        assertThat(MappingImpl.address(null));
    }
    
    @Test(expected=NullPointerException.class)
    public void regexFactoryMethodShouldThrowOnNull() {
        assertThat(MappingImpl.regex(null));
    }
    
    @Test(expected=NullPointerException.class)
    public void domainFactoryMethodShouldThrowOnNull() {
        assertThat(MappingImpl.domain(null));
    }
    
    
    @Test(expected=NullPointerException.class)
    public void errorFactoryMethodShouldThrowOnNull() {
        assertThat(MappingImpl.error(null));
    }
    
    @Test
    public void hasDomainshouldReturnTrueWhenMappingContainAtMark() {
        assertThat(MappingImpl.address("a@b").hasDomain()).isTrue();
    }
    
    @Test
    public void hasDomainshouldReturnFalseWhenMappingIsEmpty() {
        assertThat(MappingImpl.address("").hasDomain()).isFalse();
    }

    @Test
    public void hasDomainshouldReturnFalseWhenMappingIsBlank() {
        assertThat(MappingImpl.address(" ").hasDomain()).isFalse();
    }

    @Test
    public void hasDomainshouldReturnFalseWhenMappingDoesntContainAtMark() {
        assertThat(MappingImpl.address("abc").hasDomain()).isFalse();
    }
    
    @Test
    public void appendDomainShouldWorkOnValidDomain() {
        assertThat(MappingImpl.address("abc").appendDomain("domain")).isEqualTo(MappingImpl.address("abc@domain"));
    }
    
    @Test
    public void appendDomainShouldWorkWhenMappingAlreadyContainsDomains() {
        assertThat(MappingImpl.address("abc@d").appendDomain("domain")).isEqualTo(MappingImpl.address("abc@d@domain"));
    }
    
    @Test(expected=NullPointerException.class)
    public void appendDomainShouldThrowWhenNullDomain() {
        MappingImpl.address("abc@d").appendDomain(null);
    }
    
    @Test
    public void appendDomainShouldWorkWhenEmptyDomain() {
        assertThat(MappingImpl.address("abc").appendDomain("")).isEqualTo(MappingImpl.address("abc@"));
    }

    @Test
    public void getTypeShouldReturnAddressWhenNoPrefix() {
        assertThat(MappingImpl.address("abc").getType()).isEqualTo(Mapping.Type.Address);
    }

    @Test
    public void getTypeShouldReturnAddressWhenEmpty() {
        assertThat(MappingImpl.address("").getType()).isEqualTo(Mapping.Type.Address);
    }
    
    @Test
    public void getTypeShouldReturnRegexWhenRegexPrefix() {
        assertThat(MappingImpl.regex("abc").getType()).isEqualTo(Mapping.Type.Regex);
    }

    @Test
    public void getTypeShouldReturnErrorWhenErrorPrefix() {
        assertThat(MappingImpl.error("abc").getType()).isEqualTo(Mapping.Type.Error);
    }

    @Test
    public void getTypeShouldReturnDomainWhenDomainPrefix() {
        assertThat(MappingImpl.domain("abc").getType()).isEqualTo(Mapping.Type.Domain);
    }
    
    @Test(expected=IllegalStateException.class)
    public void getErrorMessageShouldThrowWhenMappingIsNotAnError() {
        MappingImpl.domain("toto").getErrorMessage();
    }
    
    @Test
    public void getErrorMessageShouldReturnMessageWhenErrorWithMessage() {
        assertThat(MappingImpl.error("toto").getErrorMessage()).isEqualTo("toto");
    }
    

    @Test
    public void getErrorMessageShouldReturnWhenErrorWithoutMessage() {
        assertThat(MappingImpl.error("").getErrorMessage()).isEqualTo("");
    }
}
