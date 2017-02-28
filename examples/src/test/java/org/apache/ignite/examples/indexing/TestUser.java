package org.apache.ignite.examples.indexing;

/**
 * <p>
 * The <code>TestUser</code>
 * </p>
 *
 * @author Alexei Scherbakov
 */
public class TestUser {
    /** */
    private String firstName;

    /** */
    private String lastName;

    /** */
    private String email;

    /** */
    private int age;

    /**
     * @param firstName First name.
     * @param lastName Last name.
     * @param email Email.
     * @param age Age.
     */
    public TestUser(String firstName, String lastName, String email, int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.age = age;
    }

    /** */
    public String getFirstName() {
        return firstName;
    }

    /** */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** */
    public String getLastName() {
        return lastName;
    }

    /** */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /** */
    public String getEmail() {
        return email;
    }

    /** */
    public void setEmail(String email) {
        this.email = email;
    }

    /** */
    public int getAge() {
        return age;
    }

    /** */
    public void setAge(int age) {
        this.age = age;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TestUser user = (TestUser)o;

        if (age != user.age)
            return false;
        if (!firstName.equals(user.firstName))
            return false;
        if (!lastName.equals(user.lastName))
            return false;
        return email.equals(user.email);

    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int result = firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + age;
        return result;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "TestUser{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                '}';
    }
}
