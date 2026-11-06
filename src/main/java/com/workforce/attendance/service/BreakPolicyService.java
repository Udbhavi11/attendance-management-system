
package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.BreakPolicyDTO;

public interface BreakPolicyService {

    BreakPolicyDTO createPolicy(BreakPolicyDTO dto);

    List<BreakPolicyDTO> getAllPolicies();

    BreakPolicyDTO getPolicyById(Long id);

    BreakPolicyDTO updatePolicy(Long id, BreakPolicyDTO dto);

    BreakPolicyDTO deactivatePolicy(Long id);

    BreakPolicyDTO getActivePolicy();
}
