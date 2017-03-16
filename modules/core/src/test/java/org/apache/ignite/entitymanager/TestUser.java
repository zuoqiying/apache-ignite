/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.entitymanager;

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

    public TestUser() {
    }

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
