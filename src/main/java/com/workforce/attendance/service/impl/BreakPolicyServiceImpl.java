
package com.workforce.attendance.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.BreakPolicyDTO;
import com.workforce.attendance.entity.BreakPolicy;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.BreakPolicyRepository;
import com.workforce.attendance.service.BreakPolicyService;

@Service
public class BreakPolicyServiceImpl implements BreakPolicyService {

    private final BreakPolicyRepository breakPolicyRepository;

    public BreakPolicyServiceImpl(BreakPolicyRepository breakPolicyRepository) {
        this.breakPolicyRepository = breakPolicyRepository;
    }

    @Override
    @Transactional
    public BreakPolicyDTO createPolicy(BreakPolicyDTO dto) {

        if (breakPolicyRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException(
                    "Break policy already exists with name: " + dto.getName());
        }

        if (dto.isActive() && breakPolicyRepository.findAll()
                .stream().anyMatch(BreakPolicy::isActive)) {
            throw new IllegalArgumentException(
                    "Another active break policy already exists");
        }

        BreakPolicy policy = new BreakPolicy();

        policy.setName(dto.getName());
        policy.setMaxBreaksPerShift(dto.getMaxBreaksPerShift());
        policy.setMaxDurationPerBreakMinutes(dto.getMaxDurationPerBreakMinutes());
        policy.setMaxTotalBreakMinutes(dto.getMaxTotalBreakMinutes());
        policy.setActive(dto.isActive());

        policy = breakPolicyRepository.save(policy);

        return convertToDTO(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BreakPolicyDTO> getAllPolicies() {

        return breakPolicyRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BreakPolicyDTO getPolicyById(Long id) {

        BreakPolicy policy = getPolicy(id);

        return convertToDTO(policy);
    }

    @Override
    @Transactional
    public BreakPolicyDTO updatePolicy(Long id, BreakPolicyDTO dto) {

        BreakPolicy policy = getPolicy(id);

        boolean duplicateName = breakPolicyRepository
                .existsByNameIgnoreCase(dto.getName());

        if (duplicateName && !policy.getName().equalsIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException(
                    "Break policy already exists with name: " + dto.getName());
        }

        if (dto.isActive() && !policy.isActive()
                && breakPolicyRepository.findAll()
                        .stream().anyMatch(BreakPolicy::isActive)) {
            throw new IllegalArgumentException(
                    "Another active break policy already exists");
        }

        policy.setName(dto.getName());
        policy.setMaxBreaksPerShift(dto.getMaxBreaksPerShift());
        policy.setMaxDurationPerBreakMinutes(dto.getMaxDurationPerBreakMinutes());
        policy.setMaxTotalBreakMinutes(dto.getMaxTotalBreakMinutes());
        policy.setActive(dto.isActive());

        policy = breakPolicyRepository.save(policy);

        return convertToDTO(policy);
    }

    @Override
    @Transactional
    public BreakPolicyDTO deactivatePolicy(Long id) {

        BreakPolicy policy = getPolicy(id);

        policy.setActive(false);

        policy = breakPolicyRepository.save(policy);

        return convertToDTO(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public BreakPolicyDTO getActivePolicy() {

        List<BreakPolicy> activePolicies = breakPolicyRepository.findAll()
                .stream()
                .filter(BreakPolicy::isActive)
                .toList();

        if (activePolicies.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No active break policy found");
        }

        if (activePolicies.size() > 1) {
            throw new IllegalStateException(
                    "Multiple active break policies found");
        }

        return convertToDTO(activePolicies.get(0));
    }

    private BreakPolicy getPolicy(Long id) {

        return breakPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Break policy not found with ID: " + id));
    }

    private BreakPolicyDTO convertToDTO(BreakPolicy policy) {

        return new BreakPolicyDTO(
                policy.getId(),
                policy.getName(),
                policy.getMaxBreaksPerShift(),
                policy.getMaxDurationPerBreakMinutes(),
                policy.getMaxTotalBreakMinutes(),
                policy.isActive()
        );
    }
}
